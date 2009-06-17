//$HeadURL$
/*----------------------------------------------------------------------------
 This file is part of deegree, http://deegree.org/
 Copyright (C) 2001-2009 by:
   Department of Geography, University of Bonn
 and
   lat/lon GmbH

 This library is free software; you can redistribute it and/or modify it under
 the terms of the GNU Lesser General Public License as published by the Free
 Software Foundation; either version 2.1 of the License, or (at your option)
 any later version.
 This library is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 details.
 You should have received a copy of the GNU Lesser General Public License
 along with this library; if not, write to the Free Software Foundation, Inc.,
 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

 Contact information:

 lat/lon GmbH
 Aennchenstr. 19, 53177 Bonn
 Germany
 http://lat-lon.de/

 Department of Geography, University of Bonn
 Prof. Dr. Klaus Greve
 Postfach 1147, 53001 Bonn
 Germany
 http://www.geographie.uni-bonn.de/deegree/

 e-mail: info@deegree.org
----------------------------------------------------------------------------*/
package org.deegree.commons.filter;

import org.deegree.commons.filter.expression.Add;
import org.deegree.commons.filter.expression.Div;
import org.deegree.commons.filter.expression.Function;
import org.deegree.commons.filter.expression.Literal;
import org.deegree.commons.filter.expression.Mul;
import org.deegree.commons.filter.expression.PropertyName;
import org.deegree.commons.filter.expression.Sub;

/**
 *
 *
 * @author <a href="mailto:schneider@lat-lon.de">Markus Schneider </a>
 * @author last edited by: $Author:$
 *
 * @version $Revision:$, $Date:$
 */
public interface Expression {

    /**
     * Convenience enum type for discriminating the different expression types.
     */
    public enum Type {
        /** Value is computed by adding two values. The {@link Expression} is an instance of {@link Add}. */
        ADD,
        /** Value is computed by subtracting two values. The {@link Expression} is an instance of {@link Sub}. */
        SUB,
        /** Value is computed by multipliying two values. The {@link Expression} is an instance of {@link Mul}. */
        MUL,
        /** Value is computed by dividing two values. The {@link Expression} is an instance of {@link Div}. */
        DIV,
        /**
         * Expression references a property of a {@link MatchableObject}. The {@link Expression} is an instance of
         * {@link PropertyName}.
         */
        PROPERTY_NAME,
        /**
         * Value is given as a literal. The {@link Expression} is an instance of {@link Literal}.
         */
        LITERAL,
        /**
         * Value is given as a function. The {@link Expression} is an instance of {@link Function}.
         */
        FUNCTION;
    }

    /**
     * Returns the type of expression. Use this to safely determine the subtype of {@link Expression}.
     *
     * @return type of expression
     */
    public Type getType();

    /**
     * Determines the value of the expression for the given {@link MatchableObject}.
     *
     * @param obj
     * @return the value of the expression
     * @throws FilterEvaluationException
     */
    public Object evaluate( MatchableObject obj )
                            throws FilterEvaluationException;

    public String toString( String indent );
}
