/* Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an
 * "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.powertac.factored.customer

import org.powertac.common.enumerations.PowerType

/**
 * @author Prashant
 *
 */
class BehaviorsProfile extends CapacityProfile
{
	ProbabilityDistribution baseTotalCapacity = null
	ProbabilityDistribution baseCapacityPerCustomer = null
	double[][] elasticityOfCapacity = null
	
	static constraints = {
		baseTotalCapacity(nullable: true)
		baseCapacityPerCustomer(nullable: true)
		elasticityOfCapacity(nullable: true)
	}
	
	/**
	 * @Override 
	 * @param xml - XML GPathResult from config
	 */
	void init(def xml, Random random) 
	{
		super.init(xml, random)

		if (xml.baseTotalCapacity.@probability.text().size() > 0) {
			baseTotalCapacity = new ProbabilityDistribution().init(xml.baseTotalCapacity, random.nextLong())
			// Note: Ignore base capacity per customer here
		} else {
			// Note: Leave baseTotalCapacity as null to indicate per customer simulation
			baseCapacityPerCustomer = new ProbabilityDistribution().init(xml.baseCapacityPerCustomer, random.nextLong())
		}
		if (xml.elasticityOfCapacity.@function.text().size() > 0) {
			elasticityOfCapacity = pairsAsDoubleArray(xml.elasticityOfCapacity.@function.text())
		}
	}
	
} // end class

