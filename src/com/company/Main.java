package com.company;

import java.io.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;

public class Main {

    public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException {
        //Rauskriegen, was �berhaupt zu tun ist
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String tnsname = args[2];
        String user = args[3];
        String pass = args[4];
        String outFileName = args[5];
        boolean flushAfterEachTable = true;

        //Mit der Datenbank verbinden
        Class oraDriver = Class.forName("oracle.jdbc.OracleDriver");
        String jdbcConnString = String.format("jdbc:oracle:thin:@//%s:%d/%s", host, port, tnsname);
        Connection oraConnection = DriverManager.getConnection(jdbcConnString, user, pass);

        //Writer �ffnen
        File outFileInfo = new File(outFileName);
        if (outFileInfo.exists())
        {
            System.out.println(String.format("%s existiert schon. Wird �berschrieben.",outFileName));
            outFileInfo.delete();
        }
        FileOutputStream fos = new FileOutputStream(outFileName);
        PrintStream writer = new PrintStream(fos,false,"UTF-8");

        //Statements, die etwas �fter gebraucht werden anlgegen.
        Statement describe = oraConnection.createStatement();
        Statement select = oraConnection.createStatement();

        //Namen aller Tabellen finden
        Statement tblListStatement = oraConnection.createStatement();
        ResultSet tblListResultSet = tblListStatement.executeQuery("SELECT table_name FROM dba_tables");
        while(tblListResultSet.next())
        {
            String tableName = tblListResultSet.getString(1);
            if (!tableName.contains("$") && !tableName.equals("PLAN_TABLE"))   //Systemeigene Tabellen, die Oracle 8 selber definiert, interessieren uns nicht.
            //if (tableName.equals("CHEM"))
            {
                System.out.println(String.format("Versuche %s zu dumpen...", tableName));
                try
                {
                    //Alle Spalten und deren Datentypen herausfinden.
                    ArrayList<Column> columnArrayList = new ArrayList<Column>();
                    ResultSet columns = oraConnection.getMetaData().getColumns(null, null, tableName, null);
                    while (columns.next())
                    {
                        String name = columns.getString("COLUMN_NAME");
                        boolean notnull = !(columns.getBoolean("NULLABLE"));
                        String dataType = columns.getString("TYPE_NAME");
                        int vcLength = columns.getInt("COLUMN_SIZE");
                        int decimalLength = columns.getInt("DECIMAL_DIGITS");
                        boolean doublette = false;

                        //Manchmal tritt das Phänomen auf, dass Oracle eine Spalte hier zwei mal listet.
                        //Das scheint entweder ein Bug in Oralce8 oder im Connector zu sein. Hier ein Workaround:
                        for(Column existing: columnArrayList)
                        {
                            if (existing.name.equals(name))
                            {
                                doublette = true;
                                continue;
                            }
                        }

                        if (doublette) continue;

                        columnArrayList.add(new Column(name, notnull, dataType, vcLength, decimalLength, false));
                    }
                    Column[] columnsArr = new Column[columnArrayList.size()];
                    columnArrayList.toArray(columnsArr);
                    Column.FixDoubles(columnsArr);
                    if (columnArrayList.size() != 0)
                    {
                        columnsArr[columnsArr.length - 1].isLast = true;
                    }
                    columns.close();

                    if (columnsArr.length == 0)
                    {
                        //Da die Tabelle keine Spalten enthält, brauchen wir uns um Zeilen nicht zu kümmern.
                        continue;
                    }

                    //Jetzt alles aus der Tabelle abfragen.
                    ResultSet rs = select.executeQuery("SELECT * FROM " + tableName);
                    boolean insertWritten = false;
                    boolean rewriteInsert = true;
                    boolean insertJustWritten = false;
                    int index = 0;
                    while(rs.next())
                    {
                        //Vor den VALUES muss ein INSERT INTO Statement kommen.
                        if (!insertWritten)
                        {
                            writer.println("DROP TABLE IF EXISTS`" + tableName + "`;");
                            writer.println("CREATE TABLE `" + tableName + "` (");
                            for (Column column : columnsArr) {
                                writer.println(column.getCreateLine());
                            }
                            writer.println(") ENGINE=InnoDB;");
                            writer.println("");

                            //LOCK TABLES benutze ich, um exklusiven Zugriff auf eine Tabelle zu bekommen.
                            writer.println("LOCK TABLES `" + tableName + "` WRITE;");
                            insertWritten = true;
                            insertJustWritten = true;
                        }
                        if (rewriteInsert)
                        {
                            //MariaDB hat die Eigenheit immer komplette INSERT Statements in den RAM zu laden.
                            //Wenn dann etwa ~3 Millionen Probenahmeparameter kommen, kann das schon mal schief gehen
                            //Deswegen nehme ich eine Trennung pro Tausend Zeilen vor.

                            if (!insertJustWritten)
                                writer.println(";");

                            writer.println("INSERT INTO `" + tableName +"`");
                            writer.println("VALUES");
                            rewriteInsert = false;
                        }
                        else
                        {
                            writer.println(",");
                        }
                        writer.print("(");
                        for (int i = 0; i < columnsArr.length; i++)
                        {
                            if (i != 0)
                            {
                                writer.print(", ");
                            }
                            try {
                                Object obj = rs.getObject(i + 1);
                                if (rs.wasNull()) {
                                    writer.print("NULL");
                                } else if (obj instanceof BigDecimal) {
                                    BigDecimal bd = (BigDecimal) obj;
                                    String bdAsString = bd.toString();
                                    writer.print(bdAsString);
                                } else if (columnsArr[i].getType() == ColumnType.INT) {
                                    writer.print((Long) obj);
                                } else if (columnsArr[i].getType() == ColumnType.VARCHAR)
                                {
                                    String theString = (String)obj;
                                    if (theString.contains("'"))
                                    {
                                        theString = theString.replace("'","''");
                                    }
                                    if (theString.contains("\\"))
                                    {
                                        theString = theString.replace("\\","\\\\");
                                    }

                                    writer.print("'" + theString + "'");
                                } else if (columnsArr[i].getType() == ColumnType.DATETIME) {
                                    try {
                                        SimpleDate theDate = new SimpleDate((Date) obj);
                                        String dateClean = theDate.toMysqlDate();
                                        writer.print("'" + dateClean + "'");
                                    }
                                    catch (BadDateException bde)
                                    {
                                        if (columnsArr[i].notnull)
                                        {
                                            SimpleDate fallback = new SimpleDate(1,1,2000);
                                            String dateClean = fallback.toMysqlDate();
                                            writer.print("'" + dateClean + "'");
                                            System.out.println(String.format("Üngültiges Datum %s auf %s abgewandelt.",obj.toString(),fallback.toMysqlDate()));
                                        }
                                        else
                                        {
                                            writer.print("NULL");
                                            System.out.println(String.format("Üngültiges Datum %s genullt.", obj.toString()));
                                        }
                                    }
                                } else if (columnsArr[i].getType() == ColumnType.BLOB)
                                {
                                    //MariaDB erwartet Binaries in Hexadezimalnotation, hier ist die Umwandlung dazu.
                                    writer.print("0x");
                                    byte[] theData = (byte[])obj;
                                    for (int j = 0; j < theData.length; j++)
                                    {
                                        writer.print(String.format("%02X",theData[j]));
                                    }
                                    System.out.println(String.format("Blob der Länge %d gelesen",theData.length));
                                }
                                else
                                {
                                        System.out.println("-> Don't know what a " + columnsArr[i].getType() + " is");
                                        throw new RuntimeException("Alright now, what the hell is this?");
                                }
                            }
                            catch (SQLException se)
                            {
                                System.out.println("-> In einer Zelle ist ein Fehler aufgetreten, sie wird genullt.");
                                writer.print("NULL");
                            }
                        }
                        writer.print(")");
                        index++;
                        if ((index % 1000) == 0)
                        {
                            System.out.println(String.format("-> %d Zeilen gelesen",index));
                            rewriteInsert = true;
                            insertJustWritten = false;
                        }
                    }
                    if (!rewriteInsert)
                    {
                        writer.println(";");
                        writer.println("UNLOCK TABLES;");
                        writer.println("");
                        if (flushAfterEachTable) {
                            writer.flush();
                        }
                    }
                    else
                    {
                        System.out.println("-> Geht aber nicht, sie ist leer.");
                    }
                }
                catch (SQLException e)
                {
                    int ecc = e.getErrorCode();
                    switch(ecc)
                    {
                        case 942:
                            System.out.println("-> Diese Tabelle existiert nicht. Vermutlich ist sie Oracle-intern.");
                            break;
                        case 911:
                            System.out.println("-> Diese Tabelle hat keinen gültigen Namen. Da sie aber in der Tabellenliste auftaucht, ist sie vermutlich intern.");
                            break;
                        default:
                            e.printStackTrace();
                            break;
                    }

                    writer.flush();
                }
            }
        }
        writer.flush();
        tblListResultSet.close();
        oraConnection.close();
    }
}
