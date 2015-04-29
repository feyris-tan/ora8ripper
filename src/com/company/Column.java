package com.company;

import java.util.ArrayList;

/**
 * Created by schiemas on 27.04.2015.
 */
public class Column
{
    /*
        String name = columns.getString("COLUMN_NAME");
        boolean notnull = !(columns.getBoolean("NULLABLE"));
        String dataType = columns.getString("TYPE_NAME");
        int vcLength = columns.getInt("COLUMN_SIZE");
        int decimalLengtgh = columns.getInt("DECIMAL_DIGITS");
     */

    String name;
    boolean notnull;
    String dataType;
    int vcLength;
    int decimalLength;
    boolean isLast;

    public Column(String name, boolean notnull, String dataType, int vcLength, int decimalLength,boolean isLast)
    {
        this.name = name;
        this.notnull = notnull;
        this.dataType = dataType;
        this.vcLength = vcLength;
        this.decimalLength = decimalLength;
        this.isLast = isLast;
    }

    public String getCreateLine()
    {
        String result = "\t";
        result += "`" + name + "` ";
        if (dataType.equals("NUMBER"))
        {
            if (decimalLength == 0)
            {
                result += "INT ";
            }
            else
            {
                result += "DECIMAL(" + vcLength + "," + decimalLength + ") ";
            }
        }
        else if (dataType.equals("FLOAT"))
        {
            result += "FLOAT ";
        }
        else if (dataType.equals("VARCHAR2"))
        {
            result += "VARCHAR(" + vcLength + ") CHARACTER SET utf8 ";
        }
        else if (dataType.equals("DATE"))
        {
            result += "DATETIME ";
        }
        else if (dataType.equals("CHAR"))
        {
            result += "VARCHAR(1) CHARACTER SET utf8 ";
        }
        else if (dataType.equals("LONG") || dataType.equals("RAW") || dataType.equals("LONG RAW"))
        {
            result += "BLOB ";
        }
        else if (dataType.equals("SDO_GEOMETRY") || dataType.equals("SDO_DIM_ARRAY"))   //Ich hab beim besten Willen keine Ahnung, was das für Typen sein sollen.
        {
            result += "GEOMETRY ";
            if (!isLast)
            {
                result += ",";
            }

            return result;
        }
        else
        {
            throw new RuntimeException("Unknown data type: " + dataType);
        }

        if (notnull)
        {
            result += " NOT NULL";
        }

        if (!isLast)
        {
            result += ",";
        }

        return result;
    }

    @Override
    public String toString() {
        return "Column{" +
                "name='" + name + '\'' +
                '}';
    }

    static void FixDoubles(Column[] cs)
    {
        if(cs.length >= 2)
        {
            if(cs[0].name.equals(cs[1].name))
            {
                cs[1].name += "2";
            }
        }
    }

    public ColumnType getType()
    {
        if (dataType.equals("NUMBER"))
        {
            if (decimalLength == 0)
            {
                return ColumnType.INT;
            }
            else
            {
                return ColumnType.DECIMAL;
            }
        }
        else if (dataType.equals("FLOAT"))
        {
            return ColumnType.FLOAT;
        }
        else if (dataType.equals("VARCHAR2"))
        {
            return ColumnType.VARCHAR;
        }
        else if (dataType.equals("DATE"))
        {
            return ColumnType.DATETIME;
        }
        else if (dataType.equals("CHAR"))
        {
            return ColumnType.VARCHAR;
        }
        else if (dataType.equals("LONG") || dataType.equals("RAW") || dataType.equals("LONG RAW"))
        {
            return ColumnType.BLOB;
        }
        else if (dataType.equals("SDO_GEOMETRY") || dataType.equals("SDO_DIM_ARRAY"))   //Ich hab beim besten Willen keine Ahnung, was das für Typen sein sollen.
        {
            return ColumnType.GEOMETRY;
        }
        else
        {
            throw new RuntimeException("Unknown data type: " + dataType);
        }

    }
}
