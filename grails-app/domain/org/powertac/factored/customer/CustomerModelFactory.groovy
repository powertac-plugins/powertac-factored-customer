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
 * Register CustomerCategory-specific creators.  Creators can be classes or closures 
 * that implement a @code{createModel()} method.
 */
class CustomerModelFactory
{
	def defaultCreator
	def customerCreators = [:]  // [CustomerCategory:CustomerCreator]
	
	
	void registerDefaultCreator(creator) 
	{
		defaultCreator = creator	
	}
	
	void registerCreator(CustomerProfile.EntityType entityType, CustomerProfile.CustomerRole customerRole, 
				         CustomerProfile.ModelType modelType, def creator) 
	{
		register(new CustomerCategory(entityType, customerRole, modelType), creator)	
	}
	
	void registerCreator(def creator)
	{
		register(creator.getCategory(), creator)
	}
	
	void registerCreator(CustomerCategory category, def creator) 
	{
		customerCreators.put(category, creator)
	}
	
	def processProfile(CustomerProfile profile) 
	{
		def creator = customerCreators.get(profile.category)
		if (creator == null) creator = defaultCreator
		return creator.createModel()
	}

} // end class

