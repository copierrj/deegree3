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
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-792
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2009.05.12 at 09:37:19 AM CEST
//


package org.deegree.commons.datasource.configuration;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * Defines the access to a database
 *
 * <p>Java class for DatabaseDataSourceType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="DatabaseDataSourceType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.deegree.org/datasource}AbstractDataSourceType">
 *       &lt;sequence>
 *         &lt;element name="ConnectionPoolId" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DatabaseDataSourceType", propOrder = {
    "connectionPoolId"
})
public class DatabaseDataSourceType
    extends AbstractDataSourceType
{

    @XmlElement(name = "ConnectionPoolId", required = true)
    protected String connectionPoolId;

    /**
     * Gets the value of the connectionPoolId property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getConnectionPoolId() {
        return connectionPoolId;
    }

    /**
     * Sets the value of the connectionPoolId property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setConnectionPoolId(String value) {
        this.connectionPoolId = value;
    }

}
