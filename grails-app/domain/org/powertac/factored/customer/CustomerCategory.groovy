/*
* Copyright 2011 the original author or authors.
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


/**
 * @author Prashant
 *
 */
class CustomerCategory
{
	CustomerProfile.EntityType entityType
	CustomerProfile.CustomerRole customerRole
	CustomerProfile.ModelType modelType
	
	CustomerCategory() {}
	
	CustomerCategory(CustomerProfile.EntityType e, CustomerProfile.CustomerRole c, CustomerProfile.ModelType m)
	{
		entityType = e
		customerRole = c
		modelType = m
	}
	
	boolean equals(def o)
	{
		return entityType==o.entityType && customerRole==o.customerRole && modelType==o.modelType
	}
}
