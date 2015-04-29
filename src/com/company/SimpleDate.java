package com.company;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Klasse, um das Arbeiten und Konvertieren von Datumsangaben zu vereinfachen.
 * @author schiemas
 *
 */
public class SimpleDate
{
    /**
     * Konstruiert ein Datum, bestehend aus Tag, Monat, Jahr
     * @param i Tag (zwischen 1-31)
     * @param j Monat (zwischen 1-12)
     * @param k Rohe Jahreszahl, beginnend ab Nach-Christi (d.h. es muss nicht erst 1970 subtrahiert werden)
     */
    public SimpleDate(int i, int j, int k)
    {
        setDay((byte) i);
        setMonth((byte)j);
        try {
            setYear((short) k);
        }
        catch (BadDateException bde)
        {
            throw new RuntimeException("Fehler beim setzen eines Datums!");
        }
    }

    /**
     * Konstruiert ein Datum aus einem java.util.Date oder einem java.sql.Date
     * @param i Das Quellobjekt
     */
    public SimpleDate(java.util.Date i) throws BadDateException {
        Calendar cal = Calendar.getInstance();
        cal.setTime(i);
        setDay((byte) cal.get(Calendar.DAY_OF_MONTH));
        setMonth((byte) cal.get(Calendar.MONTH));
        setYear((short) cal.get(Calendar.YEAR));
        hour = ((byte)cal.get(Calendar.SECOND));
        minute = ((byte)cal.get(Calendar.MINUTE));
        second = ((byte)cal.get(Calendar.SECOND));
    }

    byte day, month;
    short year;

    byte hour, minute, second;

    public byte getDay() {
        return day;
    }

    /**
     * Setzt die Tageskomponente dieses Datums. �bersch�ssige Tage werden mod 31 gerechnet.
     * @param day
     */
    public void setDay(byte day)
    {
        this.day = day;
        if (day > 31)
        {
            this.day = (byte)(day % 31);
        }
    }

    public byte getMonth() {
        return month;
    }

    /**
     * Setzt die Monatskomponente dieses Datums. �bersch�ssige Monate werden auf die
     * Jahreszahl addiert.
     * @param month
     */
    public void setMonth(byte month)
    {
        this.month = month;
        if (month > 12)
        {
            this.month = (byte)(month % 12);
            this.year += (month / 12);
        }
    }

    public short getYear() {
        return year;
    }
    public void setYear(short year) throws BadDateException {
        if (year < 100)
        {
            if (year < 30)
            {
                this.year = (short)(2000 + year);
            }
            else
            {
                this.year = (short)(1900 + year);
            }
            System.out.println(String.format("-> Üngültige Jahreszahl %d korrigiert auf %d",year,this.year));
            return;
        }
        if (year == 199)
        {
            this.year = 1990;
            System.out.println("-> Hier ist eine Jahreszahl 199. Korrigiert  auf 1990.");
            return;
        }
        if (year < 1000)
        {
            throw new BadDateException("Ein Datum muss mindestens mit dem Jahr 1970 beginnen: " + year);
        }
        this.year = year;
    }



    /**
     * Berechnet einen Hashcode aus dieser Instanz.
     * Automatisch durch Eclipse generierte Methode.
     *
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + day;
        result = prime * result + month;
        result = prime * result + year;
        return result;
    }

    /**
     * Vergleich, ob diese Instanz gleichwertig mit einer anderen ist.
     * Automatisch durch Eclipse generierte Methode.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SimpleDate other = (SimpleDate) obj;
        if (day != other.day)
            return false;
        if (month != other.month)
            return false;
        if (year != other.year)
            return false;
        return true;
    }

    /**
     * Gibt dieses Datum als String im Format MM/TT/JJJJ zur�ck.
     */
    @Override
    public String toString()
    {
        return String.format("%02d/%02d/%04d", month, day, year);
    }

    /**
     * Gibt das Datum in einem Format zur�ck, wie LmsFetcher es ben�tigt.
     * @return Das Datum, welches durch diese Instanz dargestellt wird, im Format MM/TT/JJJJ
     */
    public String toProbenahmedatum()
    {
        return toString();
    }

    /**
     * Wandelt dieses Datum in ein java.sql.Date um
     * @return Das Datum, welches durch diese Instanz dargestellt wird als java.sql.Date
     */
    public java.sql.Date toSqlDate()
    {
        //Zwar bietet Java einen Konstruktor, der die Tage day,month,year akzeptieren w�rde,
        //dieser ist jedoch als Obsolte gekennzeichnet. Leider!

        java.util.Date regularDate;
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
            regularDate = formatter.parse(toString());
            return new java.sql.Date(regularDate.getTime());
        } catch (ParseException e) {
            // Dieses absolut pingelige Exception-Handling mag ich an Java nicht...
            throw new RuntimeException("Anscheinend ist die aktuelle Java Runtime nicht in der Lage, eine Datumskonvertierung durchzuf�hren.");
        }
    }

    /**
     * Wandelt dieses Datum in ein java.util.Date um
     * @return Das Datum, welches durch diese Instanz dargestellt wird als java.util.Date
     */
    public java.util.Date toDate()
    {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
            java.util.Date regularDate = formatter.parse(toString());
            return regularDate;
        } catch (ParseException e) {
            // Dieses absolut pingelige Exception-Handling mag ich an Java nicht...
            throw new RuntimeException("Anscheinend ist die aktuelle Java Runtime nicht in der Lage, eine Datumskonvertierung durchzuf�hren.");
        }
    }

    public String toMysqlDate()
    {
        //1998-10-06 11:00:00
        String result;
        if ((hour == 0) && (minute == 0) && (second == 0))
        {
            result = String.format("%04d-%02d-%02d",year,month,day);
        }
        else
        {
            result = String.format("%04d-%02d-%02d %02d:%02d:%02d",year,month,day,hour,minute,second);
        }
        return result;
    }
}
