/*
 * -----------------------------------------------------------------------
 * Copyright © 2013-2016 Meno Hochschild, <http://www.menodata.de/>
 * -----------------------------------------------------------------------
 * This file (IsoInterval.java) is part of project Time4J.
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

import net.time4j.engine.AttributeQuery;
import net.time4j.engine.ChronoDisplay;
import net.time4j.engine.ChronoFunction;
import net.time4j.engine.Temporal;
import net.time4j.engine.TimeLine;
import net.time4j.format.Attributes;
import net.time4j.format.CalendarText;
import net.time4j.format.FormatPatternProvider;
import net.time4j.format.expert.ChronoFormatter;
import net.time4j.format.expert.ChronoPrinter;

import java.io.IOException;
import java.util.Locale;


/**
 * <p>Represents an abstract temporal interval on a timeline for
 * ISO-8601-types. </p>
 *
 * <p>Note that the start of an interval is (almost) always included (with
 * the exception of intervals with infinite past). The end is open for
 * intervals with infinite future and else included for date intervals
 * by default and excluded for other interval types. This default setting
 * can be overwritten however (although potentially harmful for the
 * performance). </p>
 *
 * @param   <T> temporal type of time points within a given interval
 * @param   <I> generic self-referencing interval type
 * @author  Meno Hochschild
 * @since   2.0
 */
/*[deutsch]
 * <p>Repr&auml;sentiert ein abstraktes Zeitintervall auf einem
 * Zeitstrahl f&uuml;r ISO-8601-Typen. </p>
 *
 * <p>Hinweis: Der Start eines Intervalls ist au&szlig;er bei Intervallen
 * mit unbegrenzter Vergangenheit (fast) immer inklusive. Das Ende eines
 * Intervalls ist bei unbegrenzter Zukunft offen, f&uuml;r Datumsintervalle
 * inklusive und sonst exklusive per Vorgabe. Diese Standardeinstellung kann
 * jedoch &uuml;berschrieben werden (obwohl potentiell sch&auml;dlich f&uuml;r
 * das Antwortzeitverhalten). </p>
 *
 * @param   <T> temporal type of time points within a given interval
 * @param   <I> generic self-referencing interval type
 * @author  Meno Hochschild
 * @since   2.0
 */
public abstract class IsoInterval<T extends Temporal<? super T>, I extends IsoInterval<T, I>>
    implements ChronoInterval<T> {

    //~ Statische Felder/Initialisierungen --------------------------------

    private static final ChronoFunction<ChronoDisplay, Void> NO_RESULT =
        new ChronoFunction<ChronoDisplay, Void>() {
            @Override
            public Void apply(ChronoDisplay context) {
                return null;
            }
        };

    //~ Instanzvariablen --------------------------------------------------

    private final Boundary<T> start;
    private final Boundary<T> end;

    //~ Konstruktoren -----------------------------------------------------

    /**
     * <p>Paket-privater Standardkonstruktor f&uuml;r Subklassen. </p>
     *
     * @param   start   untere Intervallgrenze
     * @param   end     obere Intervallgrenze
     * @throws  IllegalArgumentException if start is after end
     */
    IsoInterval(
        Boundary<T> start,
        Boundary<T> end
    ) {
        super();


        if (Boundary.isAfter(start, end)) { // NPE-check
            throw new IllegalArgumentException(
                "Start after end: " + start + "/" + end);
        } else if (
            end.isOpen() // NPE-check
            && start.isOpen()
            && Boundary.isSimultaneous(start, end)
        ) {
            if (start.isInfinite()) {
                throw new IllegalArgumentException(
                    "Infinite boundaries must not be equal.");
            } else {
                throw new IllegalArgumentException(
                    "Open start equal to open end: " + start + "/" + end);
            }
        }

        this.start = start;
        this.end = end;

    }

    //~ Methoden ----------------------------------------------------------

    @Override
    public final Boundary<T> getStart() {

        return this.start;

    }

    @Override
    public final Boundary<T> getEnd() {

        return this.end;

    }

    /**
     * <p>Yields a copy of this interval with given start time. </p>
     *
     * @param   temporal    new start timepoint
     * @return  changed copy of this interval
     * @throws  IllegalArgumentException if new start is after end
     * @since   2.0
     */
    /*[deutsch]
     * <p>Liefert eine Kopie dieses Intervalls mit der angegebenen
     * Startzeit. </p>
     *
     * @param   temporal    new start timepoint
     * @return  changed copy of this interval
     * @throws  IllegalArgumentException if new start is after end
     * @since   2.0
     */
    public I withStart(T temporal) {

        IntervalEdge edge = this.start.getEdge();
        Boundary<T> b = Boundary.of(edge, temporal);
        return this.getFactory().between(b, this.end);

    }

    /**
     * <p>Yields a copy of this interval with given end time. </p>
     *
     * @param   temporal    new end timepoint
     * @return  changed copy of this interval
     * @throws  IllegalArgumentException if new end is before start
     * @since   2.0
     */
    /*[deutsch]
     * <p>Liefert eine Kopie dieses Intervalls mit der angegebenen Endzeit. </p>
     *
     * @param   temporal    new end timepoint
     * @return  changed copy of this interval
     * @throws  IllegalArgumentException if new end is before start
     * @since   2.0
     */
    public I withEnd(T temporal) {

        IntervalEdge edge = this.end.getEdge();
        Boundary<T> b = Boundary.of(edge, temporal);
        return this.getFactory().between(this.start, b);

    }

    /**
     * <p>Excludes the upper boundary from this interval. </p>
     *
     * @return  changed copy of this interval excluding upper boundary
     * @since   2.0
     */
    /*[deutsch]
     * <p>Nimmt die obere Grenze von diesem Intervall aus. </p>
     *
     * @return  changed copy of this interval excluding upper boundary
     * @since   2.0
     */
    public I withOpenEnd() {

        Boundary<T> b;

        if (this.end.isInfinite()) {
            b = Boundary.infiniteFuture();
        } else {
            b = Boundary.of(IntervalEdge.OPEN, this.end.getTemporal());
        }

        return this.getFactory().between(this.start, b);

    }

    /**
     * <p>Includes the upper boundary of this interval. </p>
     *
     * @return  changed copy of this interval including upper boundary
     * @throws  IllegalStateException if the end is infinite future
     * @since   2.0
     */
    /*[deutsch]
     * <p>Schlie&szlig;t die obere Grenze dieses Intervall ein. </p>
     *
     * @return  changed copy of this interval including upper boundary
     * @throws  IllegalStateException if the end is infinite future
     * @since   2.0
     */
    public I withClosedEnd() {

        Boundary<T> b;

        if (this.getEnd().isInfinite()) {
            throw new IllegalStateException(
                "Infinite future cannot be included.");
        } else {
            b = Boundary.of(IntervalEdge.CLOSED, this.getEnd().getTemporal());
        }

        return this.getFactory().between(this.start, b);

    }

    @Override
    public boolean isFinite() {

        return !(this.start.isInfinite() || this.end.isInfinite());

    }

    @Override
    public boolean isEmpty() {

        if (!this.isFinite()) {
            return false;
        }

        T s = this.start.getTemporal();
        T e = this.end.getTemporal();

        if (this.start.isOpen()) {
            if (this.end.isClosed()) {
                return s.isSimultaneous(e);
            }
            s = this.getTimeLine().stepForward(s);
            if (s == null) {
                return false;
            }
        }

        return (this.end.isOpen() && s.isSimultaneous(e));

    }

    @Override
    public boolean isBefore(T temporal) {

        if (temporal == null) {
            throw new NullPointerException();
        } else if (this.end.isInfinite()) {
            return false;
        }

        T endA = this.end.getTemporal();

        if (this.end.isOpen()) {
            return !endA.isAfter(temporal);
        } else {
            return endA.isBefore(temporal);
        }

    }

    /**
     * <p>Is this interval before the other one? </p>
     *
     * <p>Equivalent to the expression
     * {@code (precedes(other) || meets(other))}. </p>
     *
     * @param   other   another interval whose relation to this interval
     *                  is to be investigated
     * @return  {@code true} if this interval is before the other one
     *          else {@code false}
     * @since   2.0
     */
    /*[deutsch]
     * <p>Liegt dieses Intervall vor dem anderen? </p>
     *
     * <p>&Auml;quivalent zum Ausdruck
     * {@code (precedes(other) || meets(other))}. </p>
     *
     * @param   other   another interval whose relation to this interval
     *                  is to be investigated
     * @return  {@code true} if this interval is before the other one
     *          else {@code false}
     * @since   2.0
     */
    public boolean isBefore(I other) {

        if (
            other.getStart().isInfinite()
            || this.end.isInfinite()
        ) {
            return false;
        }

        T endA = this.end.getTemporal();
        T startB = other.getClosedFiniteStart();

        if (this.end.isOpen()) {
            return !endA.isAfter(startB);
        } else {
            return endA.isBefore(startB);
        }

    }

    @Override
    public boolean isAfter(T temporal) {

        if (temporal == null) {
            throw new NullPointerException();
        } else if (this.start.isInfinite()) {
            return false;
        }

        return this.getClosedFiniteStart().isAfter(temporal);

    }

    /**
     * <p>Is this interval after the other one? </p>
     *
     * <p>Equivalent to the expression
     * {@code (precededBy(other) || metBy(other))}. </p>
     *
     * @param   other   another interval whose relation to this interval
     *                  is to be investigated
     * @return  {@code true} if this interval is after the other one
     *          else {@code false}
     * @since   2.0
     */
    /*[deutsch]
     * <p>Liegt dieses Intervall nach dem anderen? </p>
     *
     * <p>&Auml;quivalent zum Ausdruck
     * {@code (precededBy(other) || metBy(other))}. </p>
     *
     * @param   other   another interval whose relation to this interval
     *                  is to be investigated
     * @return  {@code true} if this interval is after the other one
     *          else {@code false}
     * @since   2.0
     */
    public boolean isAfter(I other) {

        return other.isBefore(this.getContext());

    }

    @Override
    public boolean contains(T temporal) {

        if (temporal == null) {
            throw new NullPointerException();
        }

        boolean startCondition;

        if (this.start.isInfinite()) {
            startCondition = true;
        } else if (this.start.isOpen()) {
            startCondition = this.start.getTemporal().isBefore(temporal);
        } else { // closed
            startCondition = !this.start.getTemporal().isAfter(temporal);
        }

        if (!startCondition) {
            return false; // short-cut
        }

        boolean endCondition;

        if (this.end.isInfinite()) {
            endCondition = true;
        } else if (this.end.isOpen()) {
            endCondition = this.end.getTemporal().isAfter(temporal);
        } else { // closed
            endCondition = !this.end.getTemporal().isBefore(temporal);
        }

        return endCondition;

    }

    /**
     * <p>Does this interval contain the other one? </p>
     *
     * <p>In contrast to {@link #encloses} the interval boundaries may also
     * be equal. </p>
     *
     * @param   other   another interval whose relation to this interval
     *                  is to be investigated
     * @return  {@code true} if this interval contains the other one
     *          else {@code false}
     * @since   2.0
     */
    /*[deutsch]
     * <p>Enth&auml;lt dieses Intervall das andere Intervall? </p>
     *
     * <p>Im Unterschied zu {@link #encloses} d&uuml;rfen die Grenzen der
     * Intervalle auch gleich sein. </p>
     *
     * @param   other   another interval whose relation to this interval
     *                  is to be investigated
     * @return  {@code true} if this interval contains the other one
     *          else {@code false}
     * @since   2.0
     */
    public boolean contains(I other) {

        if (!other.isFinite()) {
            return false;
        }

        T startA = this.getClosedFiniteStart();
        T startB = other.getClosedFiniteStart();

        if ((startA != null) && startA.isAfter(startB)) {
            return false;
        }

        T endA = this.end.getTemporal();
        T endB = other.getEnd().getTemporal();

        if (endA == null) {
            return true;
        }

        if (
            other.getEnd().isOpen()
            && startB.isSimultaneous(endB)
        ) {
            if (this.end.isOpen()) {
                endA = this.getTimeLine().stepBackwards(endA);
            }
            if ((endA == null) || startB.isAfter(endA)) {
                return false;
            }
        } else if (this.getFactory().isCalendrical()) {
            if (this.end.isOpen()) {
                endA = this.getTimeLine().stepBackwards(endA);
            }
            if (other.getEnd().isOpen()) {
                endB = this.getTimeLine().stepBackwards(endB);
            }
            if (
                (endA == null)
                || (endB == null) // dann startB = infinite_past
                || endA.isBefore(endB)
            ) {
                return false;
            }
        } else {
            if (this.end.isClosed()) {
                endA = this.getTimeLine().stepForward(endA);
                if (endA == null) {
                    return true;
                }
            }
            if (other.getEnd().isClosed()) {
                endB = this.getTimeLine().stepForward(endB);
                if (endB == null) {
                    return false;
                }
            }
            return !endA.isBefore(endB);
        }

        return true;

    }

    /**
     * <p>Changes this interval to an empty interval with the same
     * start anchor. </p>
     *
     * @return  new empty interval with same start (anchor always inclusive)
     * @throws  IllegalStateException if the start is infinite
     * @since   2.0
     */
    /*[deutsch]
     * <p>Wandelt dieses Intervall in ein leeres Intervall mit dem gleichen
     * Startanker um. </p>
     *
     * @return  new empty interval with same start (anchor always inclusive)
     * @throws  IllegalStateException if the start is infinite
     * @since   2.0
     */
    public I collapse() {

        if (this.start.isInfinite()) {
            throw new IllegalStateException(
                "An interval with infinite past cannot be collapsed.");
        }

        T t = this.getClosedFiniteStart();
        Boundary<T> s = Boundary.ofClosed(t);
        Boundary<T> e = Boundary.ofOpen(t);
        return this.getFactory().between(s, e);

    }

    /**
     * <p>Changes this interval to an interval such that calendrical intervals become closed intervals
     * and other intervals become half-open. </p>
     *
     * <p>The temporal space will not be changed. </p>
     *
     * @return  new interval with canonical boundaries
     * @throws  IllegalStateException if there is no canonical form (for example for [00:00/24:00])
     * @since   3.9/4.6
     */
    /*[deutsch]
     * <p>Wandelt dieses Intervall so um, da&szlig; kalendarische Intervalle geschlossen und andere
     * Intervalle halb-offen werden. </p>
     *
     * <p>Der temporale Zeitraum wird nicht ge&auml;ndert. </p>
     *
     * @return  new interval with canonical boundaries
     * @throws  IllegalStateException if there is no canonical form (for example for [00:00/24:00])
     * @since   3.9/4.6
     */
    public I toCanonical() {

        boolean change = false;
        Boundary<T> s = this.start;
        Boundary<T> e = this.end;

        if (!this.start.isInfinite() && this.start.isOpen()) {
            T t = this.getTimeLine().stepForward(this.start.getTemporal());

            if (t == null) {
                throw new IllegalStateException("Cannot canonicalize this interval: " + this);
            }

            s = Boundary.ofClosed(t);
            change = true;
        }

        if (!this.end.isInfinite()) {
            if (this.getFactory().isCalendrical()) {
                if (this.end.isOpen()) {
                    T t = this.getTimeLine().stepBackwards(this.end.getTemporal());

                    if (t == null) {
                        throw new IllegalStateException("Cannot canonicalize this interval: " + this);
                    }

                    e = Boundary.ofClosed(t);
                    change = true;
                }
            } else {
                if (this.end.isClosed()) {
                    T t = this.getTimeLine().stepForward(this.end.getTemporal());

                    if (t == null) {
                        throw new IllegalStateException("Cannot canonicalize this interval: " + this);
                    }

                    e = Boundary.ofOpen(t);
                    change = true;
                }
            }
        }

        return (change ? this.getFactory().between(s, e) : this.getContext());

    }

    /**
     * <p>Compares the boundaries (start and end) and also the time axis
     * of this and the other interval. </p>
     *
     * <p>Note: Two intervals which are {@link #equivalentTo} to each other
     * are not always equal to each other. For example a half-open date interval
     * whose end is one day later than the end of a closed date interval with
     * same start is considered <i>equivalent</i>, but not <i>equal</i>. </p>
     *
     * @param   obj     object to be compared with this
     * @return  {@code true} if given object is also an interval on the
     *          same time axis and has the same boundaries as this interval
     *          else {@code false}
     */
    /*[deutsch]
     * <p>Vergleicht die Intervallgrenzen und die assoziierte Zeitachse
     * dieses Intervalls mit denen des angegebenen Objekts. </p>
     *
     * <p>Hinweis: Zwei Intervalle, die {@link #equivalentTo} zueinander sind,
     * sind nicht immer gleich im Sinne dieser Methode. Zum Beispiel ist ein
     * halb-offenes Datumsintervall, dessen Ende einen Tag sp&auml;ter als
     * das Ende eines geschlossenen Datumsintervalls mit gleichem Start liegt,
     * <i>&auml;quivalent</i>, aber nicht <i>gleich</i>. </p>
     *
     * @param   obj     object to be compared with this
     * @return  {@code true} if given object is also an interval on the
     *          same time axis and has the same boundaries as this interval
     *          else {@code false}
     */
    @Override
    public final boolean equals(Object obj) {

        if (this == obj) {
            return true;
        } else if (obj instanceof IsoInterval) {
            IsoInterval<?, ?> that = IsoInterval.class.cast(obj);
            return (
                this.start.equals(that.start)
                && this.end.equals(that.end)
                && this.getTimeLine().equals(that.getTimeLine())
            );
        } else {
            return false;
        }

    }

    @Override
    public final int hashCode() {

        return (17 * this.start.hashCode() + 37 * this.end.hashCode());

    }

    /**
     * <p>Yields a descriptive string using the standard output
     * of the method {@code toString()} of start and end. </p>
     *
     * @return  String
     */
    /*[deutsch]
     * <p>Liefert eine Beschreibung, die auf der Standardausgabe von
     * {@code toString()} angewandt auf Start und Ende beruht.. </p>
     *
     * @return  String
     */
    @Override
    public final String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append(this.start.isOpen() ? '(' : '[');
        sb.append(
            this.start.isInfinite()
            ? "-\u221E"
            : this.start.getTemporal());
        sb.append('/');
        sb.append(
            this.end.isInfinite()
            ? "+\u221E"
            : this.end.getTemporal());
        sb.append(this.end.isOpen() ? ')' : ']');
        return sb.toString();

    }

    /**
     * <p>Prints the canonical form of this interval using a localized interval pattern. </p>
     *
     * <p>If given printer does not contain a reference to a locale then the interval pattern
     * &quot;{0}/{1}&quot; will be used. Note: Starting with version v2.0 and before v3.9/4.6,
     * this method had a different behaviour and just delegated to
     * {@code print(printer, BracketPolicy.SHOW_WHEN_NON_STANDARD)}. </p>
     *
     * @param   printer     format object for printing start and end
     * @return  localized formatted string
     * @throws  IllegalStateException if the canonicalization of this interval fails
     * @see     #toCanonical()
     * @see     #print(ChronoPrinter, String)
     * @see     FormatPatternProvider#getIntervalPattern(Locale)
     * @since   3.9/4.6
     */
    /*[deutsch]
     * <p>Formatiert die kanonische Form dieses Intervalls mit Hilfe eines lokalisierten Intervallmusters. </p>
     *
     * <p>Falls der angegebene Formatierer keine Referenz zu einer Sprach- und L&auml;ndereinstellung hat, wird
     * das Intervallmuster &quot;{0}/{1}&quot; verwendet. Hinweis: Beginnend mit Version v2.0 und vor v3.9/4.6
     * hatte diese Methode ein anderes Verhalten und delegierte einfach an
     * {@code print(printer, BracketPolicy.SHOW_WHEN_NON_STANDARD)}. </p>
     *
     * @param   printer     format object for printing start and end
     * @return  localized formatted string
     * @throws  IllegalStateException if the canonicalization of this interval fails
     * @see     #toCanonical()
     * @see     #print(ChronoPrinter, String)
     * @see     FormatPatternProvider#getIntervalPattern(Locale)
     * @since   3.9/4.6
     */
    public String print(ChronoPrinter<T> printer) {

        return this.print(printer, getIntervalPattern(printer));

    }

    /**
     * <p>Prints the canonical form of this interval in a custom format. </p>
     *
     * <p>Example: </p>
     *
     * <pre>
     *  DateInterval interval = DateInterval.since(PlainDate.of(2015, 1, 1));
     *  ChronoFormatter&lt;PlainDate&gt; formatter =
     *      ChronoFormatter.ofDatePattern(&quot;MMM d, uuuu&quot;, PatternType.CLDR, Locale.US);
     *  System.out.println(interval.print(formatter, &quot;since {0}&quot;));
     *  // output: since Jan 1, 2015
     * </pre>
     *
     * @param   printer             format object for printing start and end components
     * @param   intervalPattern     interval pattern containing placeholders {0} and {1} (for start and end)
     * @throws  IllegalStateException if the canonicalization of this interval fails
     * @return  formatted string in given pattern format
     * @see     #toCanonical()
     * @since   3.9/4.6
     */
    /*[deutsch]
     * <p>Formatiert die kanonische Form dieses Intervalls in einem benutzerdefinierten Format. </p>
     *
     * <p>Beispiel: </p>
     *
     * <pre>
     *  DateInterval interval = DateInterval.since(PlainDate.of(2015, 1, 1));
     *  ChronoFormatter&lt;PlainDate&gt; formatter =
     *      ChronoFormatter.ofDatePattern(&quot;d. MMMM uuuu&quot;, PatternType.CLDR, Locale.GERMANY);
     *  System.out.println(interval.print(formatter, &quot;seit {0}&quot;));
     *  // Ausgabe: seit 1. Januar 2015
     * </pre>
     *
     * @param   printer             format object for printing start and end components
     * @param   intervalPattern     interval pattern containing placeholders {0} and {1} (for start and end)
     * @return  formatted string in given pattern format
     * @throws  IllegalStateException if the canonicalization of this interval fails
     * @see     #toCanonical()
     * @since   3.9/4.6
     */
    public String print(
        ChronoPrinter<T> printer,
        String intervalPattern
    ) {

        I interval = this.toCanonical();
        AttributeQuery attrs = extractDefaultAttributes(printer);
        StringBuilder sb = new StringBuilder(32);
        int i = 0;
        int n = intervalPattern.length();

        try {
            while (i < n) {
                char c = intervalPattern.charAt(i);
                if ((c == '{') && (i + 2 < n) && (intervalPattern.charAt(i + 2) == '}')) {
                    char next = intervalPattern.charAt(i + 1);
                    if (next == '0') {
                        if (interval.getStart().isInfinite()) {
                            sb.append("-\u221E");
                        } else {
                            printer.print(interval.getStart().getTemporal(), sb, attrs, NO_RESULT);
                        }
                        i += 3;
                        continue;
                    } else if (next == '1') {
                        if (interval.getEnd().isInfinite()) {
                            sb.append("+\u221E");
                        } else {
                            printer.print(interval.getEnd().getTemporal(), sb, attrs, NO_RESULT);
                        }
                        i += 3;
                        continue;
                    }
                }
                sb.append(c);
                i++;
            }
        } catch (IOException ioe) {
            throw new AssertionError(ioe);
        }

        return sb.toString();

    }

    /**
     * <p>Prints the start and end separated by a slash using given formatter (technical format). </p>
     *
     * <p>Note: Infinite boundaries are printed either as &quot;-&#x221E;&quot;
     * or &quot;+&#x221E;&quot;. If given bracket policy is specified as {@code SHOW_NEVER}
     * then the canonical form of this interval will be printed. Example for an ISO-like representation: </p>
     *
     * <pre>
     *  DateInterval interval = DateInterval.since(PlainDate.of(2015, 1, 1));
     *  System.out.println(
     *      interval.print(
     *          Iso8601Format.BASIC_CALENDAR_DATE,
     *          BracketPolicy.SHOW_ALWAYS));
     *  // output: [20150101/+&#x221E;)
     * </pre>
     *
     * @param   printer     format object for printing start and end
     * @param   policy      strategy for printing interval boundaries
     * @return  formatted string in format {start}/{end}
     * @throws  IllegalStateException if the canonicalization of this interval fails
     * @since   2.0
     */
    /*[deutsch]
     * <p>Formatiert den Start und das Ende getrennt mit einem Schr&auml;gstrich
     * unter Benutzung des angegebenen Formatierers (technisches Format). </p>
     *
     * <p>Hinweis: Unendliche Intervallgrenzen werden entweder als
     * &quot;-&#x221E;&quot; oder &quot;+&#x221E;&quot; ausgegeben.
     * Wenn die angegebene {@code BracketPolicy} gleich {@code SHOW_NEVER}
     * ist, dann wird die kanonische Form dieses Intervalls ausgegeben.
     * Beispiel f&uuml;r eine ISO-&auml;hnliche Darstellung: </p>
     *
     * <pre>
     *  DateInterval interval = DateInterval.since(PlainDate.of(2015, 1, 1));
     *  System.out.println(
     *      interval.print(
     *          Iso8601Format.BASIC_CALENDAR_DATE,
     *          BracketPolicy.SHOW_ALWAYS));
     *  // output: [20150101/+&#x221E;)
     * </pre>
     *
     * @param   printer     format object for printing start and end
     * @param   policy      strategy for printing interval boundaries
     * @return  formatted string in format {start}/{end}
     * @throws  IllegalStateException if the canonicalization of this interval fails
     * @since   2.0
     */
    public String print(
        ChronoPrinter<T> printer,
        BracketPolicy policy
    ) {

        try {
            StringBuilder sb = new StringBuilder(64);
            this.print(printer, '/', printer, policy, sb);
            return sb.toString();
        } catch (IOException ioe) {
            throw new AssertionError(ioe);
        }

    }

    /**
     * <p>Prints this interval in a technical format using given formatters and separator. </p>
     *
     * <p>Note: Infinite boundaries are printed either as &quot;-&#x221E;&quot; or &quot;+&#x221E;&quot;.
     * If given bracket policy is specified as {@code SHOW_NEVER} then the canonical form of this
     * interval will be printed. </p>
     *
     * @param   startFormat format object for printing start component
     * @param   separator   char separating start and end component
     * @param   endFormat   format object for printing end component
     * @param   policy      strategy for printing interval boundaries
     * @param   buffer      writing buffer
     * @throws  IllegalStateException if the canonicalization of this interval fails
     * @throws  IOException if writing to the buffer fails
     * @since   3.9/4.6
     */
    /*[deutsch]
     * <p>Formatiert dieses Intervall in einem technischen Format unter Benutzung der angegebenen Formatierer
     * und des angegebenen Trennzeichens. </p>
     *
     * <p>Hinweis: Unendliche Intervallgrenzen werden entweder als &quot;-&#x221E;&quot; oder
     * &quot;+&#x221E;&quot; ausgegeben. Wenn die angegebene {@code BracketPolicy} gleich {@code SHOW_NEVER}
     * ist, dann wird die kanonische Form dieses Intervalls ausgegeben. </p>
     *
     * @param   startFormat format object for printing start component
     * @param   separator   char separating start and end component
     * @param   endFormat   format object for printing end component
     * @param   policy      strategy for printing interval boundaries
     * @param   buffer      writing buffer
     * @throws  IllegalStateException if the canonicalization of this interval fails
     * @throws  IOException if writing to the buffer fails
     * @since   3.9/4.6
     */
    public void print(
        ChronoPrinter<T> startFormat,
        char separator,
        ChronoPrinter<T> endFormat,
        BracketPolicy policy,
        Appendable buffer
    ) throws IOException {

        I interval = this.getContext();

        if (policy == BracketPolicy.SHOW_NEVER) {
            interval = this.toCanonical();
        }

        AttributeQuery attrs = extractDefaultAttributes(startFormat);
        boolean showBoundaries = policy.display(this);

        if (showBoundaries) {
            buffer.append(interval.getStart().isOpen() ? '(' : '[');
        }

        if (interval.getStart().isInfinite()) {
            buffer.append("-\u221E");
        } else {
            startFormat.print(interval.getStart().getTemporal(), buffer, attrs, NO_RESULT);
        }

        buffer.append(separator);

        if (interval.getEnd().isInfinite()) {
            buffer.append("+\u221E");
        } else {
            endFormat.print(interval.getEnd().getTemporal(), buffer, attrs, NO_RESULT);
        }

        if (showBoundaries) {
            buffer.append(interval.getEnd().isOpen() ? ')' : ']');
        }

    }

    /**
     * <p>ALLEN-relation: Does this interval equal the other one taking into
     * account the open or closed state of the boundaries? </p>
     *
     * <p>Relation diagram: </p>
     *
     * <p><img src="doc-files/equivalent.jpg" /></p>
     *
     * @param   other   another interval whose relation to this interval
     *                  is to be investigated
     * @return  {@code true} if this interval is temporally equivalent to
     *          the other one else {@code false}
     * @since   2.0
     */
    /*[deutsch]
     * <p>ALLEN-Relation: Ist dieses Intervall gleich dem anderen Intervall
     * unter Ber&uuml;cksichtigung des offen/geschlossen-Zustands der
     * Intervallgrenzen? </p>
     *
     * <p>Relation diagram: </p>
     *
     * <p><img src="doc-files/equivalent.jpg" /></p>
     *
     * @param   other   another interval whose relation to this interval
     *                  is to be investigated
     * @return  {@code true} if this interval is temporally equivalent to
     *          the other one else {@code false}
     * @since   2.0
     */
    public boolean equivalentTo(I other) {

        if (this.getContext() == other) {
            return true;
        }

        T startA = this.getClosedFiniteStart();
        T startB = other.getClosedFiniteStart();

        if (startA == null) {
            if (startB != null) {
                return false;
            }
        } else if (startB == null) {
            return false;
        } else if (!startA.isSimultaneous(startB)) {
            return false;
        }

        T endA = this.end.getTemporal();
        T endB = other.getEnd().getTemporal();

        if (endA == null) {
            return (endB == null);
        } else if (endB == null) {
            return false;
        }

        if (this.getFactory().isCalendrical()) {
            if (this.end.isOpen()) {
                endA = this.getTimeLine().stepBackwards(endA);
            }
            if (other.getEnd().isOpen()) {
                endB = this.getTimeLine().stepBackwards(endB);
            }
        } else {
            if (this.end.isClosed()) {
                endA = this.getTimeLine().stepForward(endA);
            }
            if (other.getEnd().isClosed()) {
                endB = this.getTimeLine().stepForward(endB);
            }
        }

        if (endA == null) {
            return (endB == null);
        } else if (endB == null) {
            return false;
        } else {
            return endA.isSimultaneous(endB);
        }

    }

    /**
     * <p>ALLEN-relation: Does this interval precede the other one such that
     * there is a gap between? </p>
     *
     * <p>Relation diagram: </p>
     *
     * <p><img src="doc-files/precedes.jpg" /></p>
     *
     * @param   other   another interval whose relation to this interval
     *                  is to be investigated
     * @return  {@code true} if this interval is before the other such
     *          that there is a gap between else {@code false}
     * @since   2.0
     * @see     #precededBy(IsoInterval)
     */
    /*[deutsch]
     * <p>ALLEN-Relation: Liegt dieses Intervall so vor dem anderen, da&szlig;
     * dazwischen eine L&uuml;cke existiert? </p>
     *
     * <p>Relation diagram: </p>
     *
     * <p><img src="doc-files/precedes.jpg" /></p>
     *
     * @param   other   another interval whose relation to this interval
     *                  is to be investigated
     * @return  {@code true} if this interval is before the other such
     *          that there is a gap between else {@code false}
     * @since   2.0
     * @see     #precededBy(IsoInterval)
     */
    public boolean precedes(I other) {

        if (
            other.getStart().isInfinite()
            || this.end.isInfinite()
        ) {
            return false;
        }

        T endA = this.end.getTemporal();

        if (this.end.isClosed()) {
            endA = this.getTimeLine().stepForward(endA);
            if (endA == null) {
                return false;
            }
        }

        return endA.isBefore(other.getClosedFiniteStart());

    }

    /**
     * <p>ALLEN-relation: Equivalent to {@code other.precedes(this)}. </p>
     *
     * <p>Relation diagram: </p>
     *
     * <p><img src="doc-files/precededBy.jpg" /></p>
     *
     * @param   other   another interval whose relation to this interval
     *                  is to be investigated
     * @return  {@code true} if this interval is after the other such
     *          that there is a gap between else {@code false}
     * @since   2.0
     * @see     #precedes(IsoInterval)
     */
    /*[deutsch]
     * <p>ALLEN-Relation: &Auml;quivalent to {@code other.precedes(this)}. </p>
     *
     * <p>Relation diagram: </p>
     *
     * <p><img src="doc-files/precededBy.jpg" /></p>
     *
     * @param   other   another interval whose relation to this interval
     *                  is to be investigated
     * @return  {@code true} if this interval is after the other such
     *          that there is a gap between else {@code false}
     * @since   2.0
     * @see     #precedes(IsoInterval)
     */
    public boolean precededBy(I other) {

        return other.precedes(this.getContext());

    }

    /**
     * <p>ALLEN-relation: Does this interval precede the other one such that
     * there is no gap between? </p>
     *
     * <p>Relation diagram: </p>
     *
     * <p><img src="doc-files/meets.jpg" /></p>
     *
     * @param   other   another interval whose relation to this interval
     *                  is to be investigated
     * @return  {@code true} if this interval is before the other such
     *          that there is no gap between else {@code false}
     * @since   2.0
     * @see     #metBy(IsoInterval)
     */
    /*[deutsch]
     * <p>ALLEN-Relation: Liegt dieses Intervall so vor dem anderen, da&szlig;
     * dazwischen keine L&uuml;cke existiert? </p>
     *
     * <p>Relation diagram: </p>
     *
     * <p><img src="doc-files/meets.jpg" /></p>
     *
     * @param   other   another interval whose relation to this interval
     *                  is to be investigated
     * @return  {@code true} if this interval is before the other such
     *          that there is no gap between else {@code false}
     * @since   2.0
     * @see     #metBy(IsoInterval)
     */
    public boolean meets(I other) {

        if (
            other.getStart().isInfinite()
            || this.end.isInfinite()
        ) {
            return false;
        }

        T endA = this.end.getTemporal();

        if (this.end.isClosed()) {
            endA = this.getTimeLine().stepForward(endA);
            if (endA == null) {
                return false;
            }
        }

        if (endA.isSimultaneous(other.getClosedFiniteStart())) {
            T startA = this.getClosedFiniteStart();
            T endB = other.getEnd().getTemporal();

            if ((startA == null) || (endB == null)) {
                return true;
            } else {
                return startA.isBefore(endB); // excludes empty.meets(empty)
            }
        }

        return false;

    }

    /**
     * <p>ALLEN-relation: Equivalent to {@code other.meets(this)}. </p>
     *
     * <p>Relation diagram: </p>
     *
     * <p><img src="doc-files/metBy.jpg" /></p>
     *
     * @param   other   another interval whose relation to this interval
     *                  is to be investigated
     * @return  {@code true} if this interval is after the other such
     *          that there is no gap between else {@code false}
     * @since   2.0
     * @see     #meets(IsoInterval)
     */
    /*[deutsch]
     * <p>ALLEN-Relation: &Auml;quivalent to {@code other.meets(this)}. </p>
     *
     * <p>Relation diagram: </p>
     *
     * <p><img src="doc-files/metBy.jpg" /></p>
     *
     * @param   other   another interval whose relation to this interval
     *                  is to be investigated
     * @return  {@code true} if this interval is after the other such
     *          that there is no gap between else {@code false}
     * @since   2.0
     * @see     #meets(IsoInterval)
     */
    public boolean metBy(I other) {

        return other.meets(this.getContext());

    }

    /**
     * <p>ALLEN-relation: Does this interval overlaps the other one such that
     * the start of this interval is still before the start of the other
     * one? </p>
     *
     * <p>Relation diagram: </p>
     *
     * <p><img src="doc-files/overlaps.jpg" /></p>
     *
     * @param   other   another interval whose relation to this interval
     *                  is to be investigated
     * @return  {@code true} if this interval overlaps the other such
     *          that the start of this interval is still before the start
     *          of the other one else {@code false}
     * @since   2.0
     * @see     #overlappedBy(IsoInterval)
     * @see     #intersects(IsoInterval)
     */
    /*[deutsch]
     * <p>ALLEN-Relation: &Uuml;berlappt dieses Intervall so das andere,
     * da&szlig; der Start dieses Intervalls noch vor dem Start des anderen
     * liegt? </p>
     *
     * <p>Relation diagram: </p>
     *
     * <p><img src="doc-files/overlaps.jpg" /></p>
     *
     * @param   other   another interval whose relation to this interval
     *                  is to be investigated
     * @return  {@code true} if this interval overlaps the other such
     *          that the start of this interval is still before the start
     *          of the other one else {@code false}
     * @since   2.0
     * @see     #overlappedBy(IsoInterval)
     * @see     #intersects(IsoInterval)
     */
    public boolean overlaps(I other) {

        if (
            other.getStart().isInfinite()
            || this.end.isInfinite()
        ) {
            return false;
        }

        T startA = this.getClosedFiniteStart();
        T startB = other.getClosedFiniteStart();

        if ((startA != null) && !startA.isBefore(startB)) {
            return false;
        }

        T endA = this.end.getTemporal();
        T endB = other.getEnd().getTemporal();

        if (this.getFactory().isCalendrical()) {
            if (this.end.isOpen()) {
                endA = this.getTimeLine().stepBackwards(endA);
            }

            if ((endA == null) || endA.isBefore(startB)) {
                return false;
            } else if (endB == null) {
                return true;
            }

            if (other.getEnd().isOpen()) {
                endB = this.getTimeLine().stepBackwards(endB);
            }
        } else {
            if (this.end.isClosed()) {
                endA = this.getTimeLine().stepForward(endA);
                if (endA == null) {
                    return (endB == null);
                }
            }

            if (!endA.isAfter(startB)) {
                return false;
            }

            if (other.getEnd().isClosed()) {
                endB = this.getTimeLine().stepForward(endB);
            }
        }

        return ((endB == null) || endA.isBefore(endB));

    }

    /**
     * <p>ALLEN-relation: Equivalent to {@code other.overlaps(this)}. </p>
     *
     * <p>Relation diagram: </p>
     *
     * <p><img src="doc-files/overlappedBy.jpg" /></p>
     *
     * @param   other   another interval whose relation to this interval
     *                  is to be investigated
     * @return  {@code true} if the other interval overlaps this such
     *          that the start of the other one is still before the start
     *          of this interval else {@code false}
     * @since   2.0
     * @see     #overlaps(IsoInterval)
     * @see     #intersects(IsoInterval)
     */
    /*[deutsch]
     * <p>ALLEN-Relation: &Auml;quivalent to {@code other.overlaps(this)}. </p>
     *
     * <p>Relation diagram: </p>
     *
     * <p><img src="doc-files/overlappedBy.jpg" /></p>
     *
     * @param   other   another interval whose relation to this interval
     *                  is to be investigated
     * @return  {@code true} if the other interval overlaps this such
     *          that the start of the other one is still before the start
     *          of this interval else {@code false}
     * @since   2.0
     * @see     #overlaps(IsoInterval)
     * @see     #intersects(IsoInterval)
     */
    public boolean overlappedBy(I other) {

        return other.overlaps(this.getContext());

    }

    /**
     * <p>ALLEN-relation: Does this interval finish the other one such that
     * both end time points are equal and the start of this interval is after
     * the start of the other one? </p>
     *
     * <p>Relation diagram: </p>
     *
     * <p><img src="doc-files/finishes.jpg" /></p>
     *
     * @param   other   another interval whose relation to this interval
     *                  is to be investigated
     * @return  {@code true} if this interval has the same end point as
     *          the other one and a later start else {@code false}
     * @since   2.0
     * @see     #finishedBy(IsoInterval)
     */
    /*[deutsch]
     * <p>ALLEN-Relation: Beendet dieses Intervall so das andere, da&szlig; bei
     * gleichen Endzeitpunkten der Start dieses Intervalls nach dem Start des
     * anderen liegt? </p>
     *
     * <p>Relation diagram: </p>
     *
     * <p><img src="doc-files/finishes.jpg" /></p>
     *
     * @param   other   another interval whose relation to this interval
     *                  is to be investigated
     * @return  {@code true} if this interval has the same end point as
     *          the other one and a later start else {@code false}
     * @since   2.0
     * @see     #finishedBy(IsoInterval)
     */
    public boolean finishes(I other) {

        if (this.start.isInfinite()) {
            return false;
        }

        T startA = this.getClosedFiniteStart();
        T startB = other.getClosedFiniteStart();
        T endA = this.end.getTemporal();
        T endB = other.getEnd().getTemporal();

        boolean empty = (
            this.end.isOpen()
            && (endA != null)
            && startA.isSimultaneous(endA)
        );

        if (
            empty
            || ((startB != null) && !startB.isBefore(startA))
        ) {
            return false;
        }

        if (endB == null) {
            return (endA == null);
        }
        if (endA == null) {
            return (endB == null);
        }

        if (this.getFactory().isCalendrical()) {
            if (this.end.isOpen()) {
                endA = this.getTimeLine().stepBackwards(endA);
            }
            if (other.getEnd().isOpen()) {
                endB = this.getTimeLine().stepBackwards(endB);
            }

            if ((endA == null) || (endB == null) || startA.isAfter(endB)) {
                return false;
            }
        } else {
            if (this.end.isClosed()) {
                endA = this.getTimeLine().stepForward(endA);
            }
            if (other.getEnd().isClosed()) {
                endB = this.getTimeLine().stepForward(endB);
            }

            if ((endB != null) && !startA.isBefore(endB)) {
                return false;
            }

            if (endA == null) {
                return (endB == null);
            } else if (endB == null) {
                return false;
            }
        }

        return endA.isSimultaneous(endB);

    }

    /**
     * <p>ALLEN-relation: Equivalent to {@code other.finishes(this)}. </p>
     *
     * <p>Relation diagram: </p>
     *
     * <p><img src="doc-files/finishedBy.jpg" /></p>
     *
     * @param   other   another interval whose relation to this interval
     *                  is to be investigated
     * @return  {@code true} if this interval has the same end point as
     *          the other one and an earlier start else {@code false}
     * @since   2.0
     * @see     #finishes(IsoInterval)
     */
    /*[deutsch]
     * <p>ALLEN-Relation: &Auml;quivalent to {@code other.finishes(this)}. </p>
     *
     * <p>Relation diagram: </p>
     *
     * <p><img src="doc-files/finishedBy.jpg" /></p>
     *
     * @param   other   another interval whose relation to this interval
     *                  is to be investigated
     * @return  {@code true} if this interval has the same end point as
     *          the other one and an earlier start else {@code false}
     * @since   2.0
     * @see     #finishes(IsoInterval)
     */
    public boolean finishedBy(I other) {

        return other.finishes(this.getContext());

    }

    /**
     * <p>ALLEN-relation: Does this interval start the other one such that both
     * start time points are equal and the end of this interval is before the
     * end of the other one? </p>
     *
     * <p>Relation diagram: </p>
     *
     * <p><img src="doc-files/starts.jpg" /></p>
     *
     * @param   other   another interval whose relation to this interval
     *                  is to be investigated
     * @return  {@code true} if this interval has the same start point as
     *          the other one and an earlier end else {@code false}
     * @since   2.0
     * @see     #startedBy(IsoInterval)
     */
    /*[deutsch]
     * <p>ALLEN-Relation: Beginnt dieses Intervall so das andere, da&szlig;
     * bei gleichen Beginnzeitpunkten das Ende dieses Intervalls vor dem Ende
     * des anderen liegt? </p>
     *
     * <p>Relation diagram: </p>
     *
     * <p><img src="doc-files/starts.jpg" /></p>
     *
     * @param   other   another interval whose relation to this interval
     *                  is to be investigated
     * @return  {@code true} if this interval has the same start point as
     *          the other one and an earlier end else {@code false}
     * @since   2.0
     * @see     #startedBy(IsoInterval)
     */
    public boolean starts(I other) {

        if (this.end.isInfinite()) {
            return false;
        }

        T startA = this.getClosedFiniteStart();
        T startB = other.getClosedFiniteStart();

        if (startB == null) {
            if (startA != null) {
                return false;
            }
        } else if (startA == null) {
            if (startB != null) {
                return false;
            }
        } else if (!startA.isSimultaneous(startB)) {
            return false;
        }

        T endA = this.end.getTemporal();
        T endB = other.getEnd().getTemporal();

        if (
            this.end.isOpen()
            && (startA != null)
            && startA.isSimultaneous(endA)
        ) {
            return true;
        }

        if (endB == null) {
            if (this.end.isClosed()) {
                return true;
            } else if (startB == null) {
                return (this.getTimeLine().stepBackwards(endA) != null);
            } else {
                return endA.isAfter(startB);
            }
        }

        if (this.getFactory().isCalendrical()) {
            if (this.end.isOpen()) {
                endA = this.getTimeLine().stepBackwards(endA);
            }
            if (other.getEnd().isOpen()) {
                endB = this.getTimeLine().stepBackwards(endB);
            }
            if ((endA == null) || (endB == null) || !endA.isBefore(endB)) {
                return false;
            }
        } else {
            if (this.end.isClosed()) {
                endA = this.getTimeLine().stepForward(endA);
                if (endA == null) {
                    return false;
                }
            }
            if (other.getEnd().isClosed()) {
                endB = this.getTimeLine().stepForward(endB);
            }
            if ((endB != null) && !endA.isBefore(endB)) {
                return false;
            }
        }

        if (this.end.isClosed()) {
            return true;
        } else if (startB == null) {
            return (this.getTimeLine().stepBackwards(endA) != null);
        } else {
            return endA.isAfter(startB);
        }

    }

    /**
     * <p>ALLEN-relation: Equivalent to {@code other.starts(this)}. </p>
     *
     * <p>Relation diagram: </p>
     *
     * <p><img src="doc-files/startedBy.jpg" /></p>
     *
     * @param   other   another interval whose relation to this interval
     *                  is to be investigated
     * @return  {@code true} if this interval has the same start point as
     *          the other one and a later end else {@code false}
     * @since   2.0
     * @see     #starts(IsoInterval)
     */
    /*[deutsch]
     * <p>ALLEN-Relation: &Auml;quivalent to {@code other.starts(this)}. </p>
     *
     * <p>Relation diagram: </p>
     *
     * <p><img src="doc-files/startedBy.jpg" /></p>
     *
     * @param   other   another interval whose relation to this interval
     *                  is to be investigated
     * @return  {@code true} if this interval has the same start point as
     *          the other one and a later end else {@code false}
     * @since   2.0
     * @see     #starts(IsoInterval)
     */
    public boolean startedBy(I other) {

        return other.starts(this.getContext());

    }

    /**
     * <p>ALLEN-relation: Does this interval enclose the other one such that
     * this start is before the start of the other one and this end is after
     * the end of the other one? </p>
     *
     * <p>Relation diagram: </p>
     *
     * <p><img src="doc-files/encloses.jpg" /></p>
     *
     * @param   other   another interval whose relation to this interval
     *                  is to be investigated
     * @return  {@code true} if this interval has the earlier start point and
     *          later end compared to the other one else {@code false}
     * @since   2.0
     * @see     #enclosedBy(IsoInterval)
     */
    /*[deutsch]
     * <p>ALLEN-Relation: Umfasst dieses Intervall so das andere, da&szlig;
     * der Start dieses Intervalls vor dem Start des anderen und das Ende
     * dieses Intervalls nach dem Ende des anderen Intervalls liegt? </p>
     *
     * <p>Relation diagram: </p>
     *
     * <p><img src="doc-files/encloses.jpg" /></p>
     *
     * @param   other   another interval whose relation to this interval
     *                  is to be investigated
     * @return  {@code true} if this interval has the earlier start point and
     *          later end compared to the other one else {@code false}
     * @since   2.0
     * @see     #enclosedBy(IsoInterval)
     */
    public boolean encloses(I other) {

        if (!other.isFinite()) {
            return false;
        }

        T startA = this.getClosedFiniteStart();
        T startB = other.getClosedFiniteStart();

        if ((startA != null) && !startA.isBefore(startB)) {
            return false;
        }

        T endA = this.end.getTemporal();
        T endB = other.getEnd().getTemporal();

        if (endA == null) {
            return true;
        }

        if (
            other.getEnd().isOpen()
            && startB.isSimultaneous(endB)
        ) {
            if (this.end.isOpen()) {
                endA = this.getTimeLine().stepBackwards(endA);
            }
            if ((endA == null) || startB.isAfter(endA)) {
                return false; // if startB == endA: interval B has zero duration
            }
        } else if (this.getFactory().isCalendrical()) {
            if (this.end.isOpen()) {
                endA = this.getTimeLine().stepBackwards(endA);
            }
            if (other.getEnd().isOpen()) {
                endB = this.getTimeLine().stepBackwards(endB);
            }
            if (
                (endA == null)
                || (endB == null) // dann startB = infinite_past
                || !endA.isAfter(endB)
            ) {
                return false;
            }
        } else {
            if (this.end.isClosed()) {
                endA = this.getTimeLine().stepForward(endA);
            }
            if (other.getEnd().isClosed()) {
                endB = this.getTimeLine().stepForward(endB);
                if (endB == null) {
                    return false;
                }
            }
            if ((endA != null) && !endA.isAfter(endB)) {
                return false;
            }
        }

        return true;

    }

    /**
     * <p>ALLEN-relation: Equivalent to {@code other.encloses(this)}. </p>
     *
     * <p>Relation diagram: </p>
     *
     * <p><img src="doc-files/enclosedBy.jpg" /></p>
     *
     * @param   other   another interval whose relation to this interval
     *                  is to be investigated
     * @return  {@code true} if this interval has the later start point and
     *          earlier end compared to the other one else {@code false}
     * @since   2.0
     * @see     #encloses(IsoInterval)
     */
    /*[deutsch]
     * <p>ALLEN-Relation: &Auml;quivalent to {@code other.encloses(this)}. </p>
     *
     * <p>Relation diagram: </p>
     *
     * <p><img src="doc-files/enclosedBy.jpg" /></p>
     *
     * @param   other   another interval whose relation to this interval
     *                  is to be investigated
     * @return  {@code true} if this interval has the later start point and
     *          earlier end compared to the other one else {@code false}
     * @since   2.0
     * @see     #encloses(IsoInterval)
     */
    public boolean enclosedBy(I other) {

        return other.encloses(this.getContext());

    }

    /**
     * <p>Queries if this interval intersects the other one such that there is at least one common time point. </p>
     *
     * @param   other   another interval which might have an intersection with this interval
     * @return  {@code true} if there is an non-empty intersection of this interval and the other one else {@code false}
     * @since   3.19/4.15
     * @see     #overlaps(IsoInterval)
     * @see     #isBefore(IsoInterval)
     * @see     #isAfter(IsoInterval)
     */
    /*[deutsch]
     * <p>Ermittelt, ob dieses Intervall sich mit dem angegebenen Intervall so &uuml;berschneidet, da&szlig;
     * mindestens ein gemeinsamer Zeitpunkt existiert. </p>
     *
     * @param   other   another interval which might have an intersection with this interval
     * @return  {@code true} if there is an non-empty intersection of this interval and the other one else {@code false}
     * @since   3.19/4.15
     * @see     #overlaps(IsoInterval)
     * @see     #isBefore(IsoInterval)
     * @see     #isAfter(IsoInterval)
     */
    public boolean intersects(I other) {

        if (this.isEmpty() || other.isEmpty()) {
            return false;
        }

        return !(this.isBefore(other) || this.isAfter(other));

    }

    /**
     * <p>Queries if this interval abuts the other one such that there is neither any overlap nor any gap between. </p>
     *
     * <p>Equivalent to the expression {@code this.meets(other) || this.metBy(other)}. </p>
     *
     * @param   other   another interval which might abut this interval
     * @return  {@code true} if there is no intersection and no gap between else {@code false}
     * @since   3.19/4.15
     * @see     #meets(IsoInterval)
     * @see     #metBy(IsoInterval)
     */
    /*[deutsch]
     * <p>Ermittelt, ob dieses Intervall das angegebene Intervall so ber&uuml;hrt, da&szlig;
     * weder eine &Uuml;berlappung noch eine L&uuml;cke dazwischen existieren. </p>
     *
     * <p>&Auml;quivalent zum Ausdruck {@code this.meets(other) || this.metBy(other)}. </p>
     *
     * @param   other   another interval which might abut this interval
     * @return  {@code true} if there is no intersection and no gap between else {@code false}
     * @since   3.19/4.15
     * @see     #meets(IsoInterval)
     * @see     #metBy(IsoInterval)
     */
    public boolean abuts(I other) {

        return (this.meets(other) || this.metBy(other));

    }

    /**
     * <p>Liefert die zugeh&ouml;rige Zeitachse. </p>
     *
     * @return  associated {@code TimeLine}
     */
    abstract TimeLine<T> getTimeLine();

    /**
     * <p>Liefert die zugeh&ouml;rige Fabrik. </p>
     *
     * @return  IntervalFactory
     */
    abstract IntervalFactory<T, I> getFactory();

    /**
     * <p>Liefert die Rechenbasis zur Ermittlung einer Dauer. </p>
     *
     * @return  &auml;quivalenter Zeitpunkt bei geschlossener unterer Grenze
     * @throws  UnsupportedOperationException wenn unendlich
     */
    T getTemporalOfClosedStart() {

        T temporal = this.start.getTemporal();

        if (temporal == null) {
            throw new UnsupportedOperationException(
                "An infinite interval has no finite duration.");
        } else if (this.start.isOpen()) {
            return this.getTimeLine().stepForward(temporal);
        } else {
            return temporal;
        }

    }

    /**
     * <p>Liefert die Startbasis f&uuml;r Vergleiche. </p>
     *
     * @return  &auml;quivalenter Zeitpunkt bei geschlossener unterer Grenze
     */
    T getClosedFiniteStart() {

        T temporal = this.start.getTemporal();

        if (
            (temporal != null)
            && this.start.isOpen()
        ) {
            return this.getTimeLine().stepForward(temporal);
        } else {
            return temporal;
        }

    }

    /**
     * <p>Liefert die Rechenbasis zur Ermittlung einer Dauer. </p>
     *
     * @return  &auml;quivalenter Zeitpunkt bei offener oberer Grenze oder
     *          {@code null} wenn angewandt auf das geschlossene Maximum
     * @throws  UnsupportedOperationException wenn unendlich
     */
    T getTemporalOfOpenEnd() {

        T temporal = this.end.getTemporal();

        if (temporal == null) {
            throw new UnsupportedOperationException(
                "An infinite interval has no finite duration.");
        } else if (this.end.isClosed()) {
            return this.getTimeLine().stepForward(temporal);
        } else {
            return temporal;
        }

    }

//    /**
//     * <p>Liefert die Endbasis f&uuml;r Vergleiche. </p>
//     *
//     * @return  &auml;quivalenter Zeitpunkt bei offener oberer Grenze oder
//     *          {@code null} wenn angewandt auf das geschlossene Maximum
//     */
/*
    T getOpenFiniteEnd() {

        T temporal = this.end.getTemporal();

        if (
            (temporal != null)
            && this.end.isClosed()
        ) {
            return this.getTimeLine().stepForward(temporal);
        } else {
            return temporal;
        }

    }
*/

    /**
     * <p>Bestimmt die Standard-Attribute, wenn das Argument ein
     * Format-Objekt ist. </p>
     *
     * @param   obj     object possibly containing format attributes
     * @return  attribute query
     */
    static AttributeQuery extractDefaultAttributes(Object obj) {

        if (obj instanceof ChronoFormatter) {
            ChronoFormatter<?> fmt = ChronoFormatter.class.cast(obj);
            return fmt.getAttributes();
        } else {
            return Attributes.empty();
        }

    }

    /**
     * <p>Yields the best available format pattern. </p>
     *
     * @return  localized format pattern for intervals
     * @since   3.9/4.6
     */
    static String getIntervalPattern(Object formatter) {

        String pattern = "{0}/{1}";

        if (formatter instanceof ChronoFormatter) {
            Locale locale = ChronoFormatter.class.cast(formatter).getLocale();
            pattern = CalendarText.patternForInterval(locale);
        }

        return pattern;

    }

    /**
     * <p>Liefert den Selbstbezug. </p>
     *
     * @return  this instance
     */
    abstract I getContext();

}
