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
//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.3 in JDK 1.6
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2008.07.01 at 03:49:29 PM CEST
//


package org.deegree.geometry.configuration;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for SupportedCurveInterpolationType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="SupportedCurveInterpolationType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="CurveInterpolation" maxOccurs="unbounded">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *               &lt;enumeration value="linear"/>
 *               &lt;enumeration value="geodesic"/>
 *               &lt;enumeration value="circularArc3Points"/>
 *               &lt;enumeration value="circularArc2PointWithBulge"/>
 *               &lt;enumeration value="elliptical"/>
 *               &lt;enumeration value="conic"/>
 *               &lt;enumeration value="cubicSpline"/>
 *               &lt;enumeration value="polynomialSpline"/>
 *               &lt;enumeration value="rationalSpline"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SupportedCurveInterpolationType", propOrder = {
    "curveInterpolation"
})
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2008-07-01T03:49:29+02:00", comments = "JAXB RI vJAXB 2.1.3 in JDK 1.6")
public class SupportedCurveInterpolationType {

    @XmlElement(name = "CurveInterpolation", required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2008-07-01T03:49:29+02:00", comments = "JAXB RI vJAXB 2.1.3 in JDK 1.6")
    protected List<String> curveInterpolation;

    /**
     * Gets the value of the curveInterpolation property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the curveInterpolation property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCurveInterpolation().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     *
     *
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2008-07-01T03:49:29+02:00", comments = "JAXB RI vJAXB 2.1.3 in JDK 1.6")
    public List<String> getCurveInterpolation() {
        if (curveInterpolation == null) {
            curveInterpolation = new ArrayList<String>();
        }
        return this.curveInterpolation;
    }

}
