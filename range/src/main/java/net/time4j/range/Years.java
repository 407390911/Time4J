/*
 * -----------------------------------------------------------------------
 * Copyright © 2013-2016 Meno Hochschild, <http://www.menodata.de/>
 * -----------------------------------------------------------------------
 * This file (Years.java) is part of project Time4J.
 *
 * Time4J is free software: You can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * Time4J is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Time4J. If not, see <http://www.gnu.org/licenses/>.
 * -----------------------------------------------------------------------
 */

package net.time4j.range;

import net.time4j.CalendarUnit;
import net.time4j.IsoDateUnit;
import net.time4j.Weekcycle;
import net.time4j.base.MathUtils;
import net.time4j.engine.TimePoint;

import java.text.ParseException;


/**
 * <p>Represents a time span in gregorian or week-based years. </p>
 *
 * @param   <U> generic type of year units
 * @author  Meno Hochschild
 * @since   3.21/4.17
 */
/*[deutsch]
 * <p>Repr&auml;sentiert eine Zeitspanne in gregorianischen oder wochenbasierten Jahren. </p>
 *
 * @param   <U> generic type of year units
 * @author  Meno Hochschild
 * @since   3.21/4.17
 */
public final class Years<U extends IsoDateUnit>
    extends SingleUnitTimeSpan<U, Years<U>> {

    //~ Statische Felder/Initialisierungen --------------------------------

    /**
     * <p>Constant for zero gregorian years. </p>
     */
    /*[deutsch]
     * <p>Konstante f&uuml;r null gregorianische Jahre. </p>
     */
    public static final Years<CalendarUnit> ZERO = new Years<CalendarUnit>(0, CalendarUnit.YEARS);

    /**
     * <p>Constant for exactly one gregorian year. </p>
     */
    /*[deutsch]
     * <p>Konstante f&uuml;r genau ein gregorianisches Jahr. </p>
     */
    public static final Years<CalendarUnit> ONE = new Years<CalendarUnit>(1, CalendarUnit.YEARS);

    private static final long serialVersionUID = 6288717039772347252L;

    //~ Konstruktoren -----------------------------------------------------

    private Years(
        int amount,
        U unit
    ) {
        super(amount, unit);

    }

    //~ Methoden ----------------------------------------------------------

    /**
     * <p>Obtains a time span in given gregorian years. </p>
     *
     * @param   years       count of gregorian years, maybe negative
     * @return  time span in years
     * @see     CalendarUnit#YEARS
     */
    /*[deutsch]
     * <p>Erh&auml;lt eine Zeitspanne in den angegebenen gregorianischen Jahren. </p>
     *
     * @param   years       count of gregorian years, maybe negative
     * @return  time span in years
     * @see     CalendarUnit#YEARS
     */
    public static Years<CalendarUnit> ofGregorian(int years) {

        return ((years == 0) ? ZERO : (years == 1) ? ONE : new Years<CalendarUnit>(years, CalendarUnit.YEARS));

    }

    /**
     * <p>Obtains a time span in given week-based years. </p>
     *
     * <p>Week-based years have a length of either 364 or 371 days. </p>
     *
     * @param   years       count of week-based years, maybe negative
     * @return  time span in years
     * @see     CalendarUnit#weekBasedYears()
     */
    /*[deutsch]
     * <p>Erh&auml;lt eine Zeitspanne in den angegebenen wochenbasierten Jahren. </p>
     *
     * <p>Wochenbasierte Jahre haben eine L&auml;nge von entweder 364 oder 371 Tagen. </p>
     *
     * @param   years       count of week-based years, maybe negative
     * @return  time span in years
     * @see     CalendarUnit#weekBasedYears()
     */
    public static Years<Weekcycle> ofWeekBased(int years) {

        return new Years<Weekcycle>(years, Weekcycle.YEARS);

    }

    /**
     * <p>Determines the temporal distance between given dates/time-points in gregorian years. </p>
     *
     * @param   <T> generic type of time-points
     * @param   t1      first time-point
     * @param   t2      second time-point
     * @return  result of year difference
     * @see     net.time4j.PlainDate
     * @see     net.time4j.PlainTimestamp
     */
    /*[deutsch]
     * <p>Bestimmt die gregorianische Jahresdifferenz zwischen den angegebenen Zeitpunkten. </p>
     *
     * @param   <T> generic type of time-points
     * @param   t1      first time-point
     * @param   t2      second time-point
     * @return  result of year difference
     * @see     net.time4j.PlainDate
     * @see     net.time4j.PlainTimestamp
     */
    public static <T extends TimePoint<? super CalendarUnit, T>> Years<CalendarUnit> between(T t1, T t2) {

        long delta = CalendarUnit.YEARS.between(t1, t2);
        return Years.ofGregorian(MathUtils.safeCast(delta));

    }

    /**
     * <p>Parses the canonical ISO-8601-format &quot;PnY&quot; with possible preceding minus-char. </p>
     *
     * @param   period      the formatted string to be parsed
     * @return  parsed instance
     * @throws  ParseException if given argument cannot be parsed
     */
    /*[deutsch]
     * <p>Interpretiert das kanonische ISO-8601-Format &quot;PnY&quot; mit optionalem vorangehenden Minus-Zeichen. </p>
     *
     * @param   period      the formatted string to be parsed
     * @return  parsed instance
     * @throws  ParseException if given argument cannot be parsed
     */
    public static Years<CalendarUnit> parseGregorian(String period) throws ParseException {

        int amount = SingleUnitTimeSpan.parsePeriod(period, 'Y');
        return Years.ofGregorian(amount);

    }

    /**
     * <p>Like {@code parseGregorian(period)} but interpretes years as week-based. </p>
     *
     * @param   period      the formatted string to be parsed
     * @return  parsed instance
     * @throws  ParseException if given argument cannot be parsed
     * @see     #parseGregorian(String)
     * @see     CalendarUnit#weekBasedYears()
     */
    /*[deutsch]
     * <p>Wie {@code parseGregorian(period)}, aber mit wochenbasierten Jahren. </p>
     *
     * @param   period      the formatted string to be parsed
     * @return  parsed instance
     * @throws  ParseException if given argument cannot be parsed
     * @see     #parseGregorian(String)
     * @see     CalendarUnit#weekBasedYears()
     */
    public static Years<Weekcycle> parseWeekBased(String period) throws ParseException {

        int amount = SingleUnitTimeSpan.parsePeriod(period, 'Y');
        return Years.ofWeekBased(amount);

    }

    @Override
    Years<U> with(int amount) {

        return new Years<U>(amount, this.getUnit());

    }

    @Override
    Years<U> self() {

        return this;

    }

    @Override
    void checkConsistency(U unit) {

        if (!unit.equals(CalendarUnit.YEARS) && !unit.equals(Weekcycle.YEARS)) {
            throw new IllegalArgumentException("Invalid year unit: " + unit);
        }

    }

}
