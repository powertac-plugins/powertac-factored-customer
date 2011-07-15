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



import java.util.List;
import org.powertac.common.enumerations.*;
import org.powertac.common.CustomerInfo
import org.powertac.common.AbstractCustomer
import org.powertac.common.Tariff;
import org.powertac.common.Timeslot
import org.powertac.tariffmarket.TariffMarketService;
import org.powertac.common.interfaces.NewTariffListener;
import org.powertac.common.TariffSubscription;

/**
* @author Prashant Reddy, CMU
*/

class OfficePopulation extends AbstractCustomer /**implements NewTariffListener**/ {
	
	Random rand = new Random() // TEMP FIXME (used for bootstrap data)
	
	// Factors:  
	
	// Hierarchical model
	
	static double[] tempFactorByMonth
	double[] consumptionByDay
	double[] consumptionByHour
	
    static constraints = {
    }
	
	void init() 
	{
		super.init()
		initConsumptionDistributions()
		this.save()
	}

	static void initConsumptionDistributions()
	{	
		final int numMonths = 12
		double[] avgTemps = [35.1, 38.8, 49.5, 60.7, 70.8, 79.1, 82.7, 81.1, 74.2, 62.5, 50.5, 39.8, 60.4]
		def roomTempFloor = 60
		def roomTempCeiling = 75
		def tempDeviations = new double[numMonths]
		double sumDeviations = 0
		for (int i = 0; i < numMonths; i++) {
			def coldness = Math.max(0, roomTempFloor - avgTemps[i])	
			def hotness = Math.max(0, avgTemps[i] - roomTempCeiling)
			tempDeviations[i] = Math.max(coldness, hotness)
			sumDeviations += tempDeviations[i]
		}
		System.out.println(tempDeviations)
		
		tempFactorByMonth = new double[numMonths]
		for (int i = 0; i < numMonths; i++) {
			tempFactorByMonth[i] = tempDeviations[i] / sumDeviations
		}
		System.out.println(tempFactorByMonth)
		
	}
	
	void consumePower()
	{
		Timeslot timeslot =  Timeslot.currentTimeslot()
		
		subscriptions.each { sub ->
					
			log.info "Consumption Load: ${summary} / ${subscriptions.size()} "
			//sub.usePower(summary/subscriptions.size())
		}
	}

	public static void main(String [ ] args)
	{
		initConsumptionDistributions()
	}
	
	
	
	
	//	OfficePopulation(String name, int custCount) {
	//		customerInfo = new CustomerInfo(name: name, population: custCount,
	//			           			        customerType: CustomerType.CustomerOffice,
	//										powerTypes: [PowerType.CONSUMPTION, PowerType.PRODUCTION])
	//		custId = customerInfo.getId()
	//		customerInfo.save()
	//		this.save()
	//	}
	//
	//	void init() {
	//		super.init()
	//	}
		
	//	void persist() {
	//		super.persist()
	//		assert(this.save())
	//	}
		
	//	/**
	//	 * Called by {@code FactoredCustomerService}
	//	 */
	//	void init(TariffMarketService tms) {
	//		super.init()
	//
	//		tms.registerNewTariffListener(this)
	//	}
	
		/***********
		@Override
		void possibilityEvaluationNewTariffs(List<Tariff> newTariffs)
		{
			System.out.println("***** OfficePopn.possibilityEvaluationNewTariffs called")
			log.info "***** OfficePopn.possibilityEvaluationNewTariffs called"
			System.out.println(subscriptions)
			
	//		if (subscriptions == null || subscriptions.size() == 0) {
				subscribeDefault()
	//		}
		}
		
		// FIXME
		static int sticky = 0
		
		@Override
		void subscribeDefault() {
			System.out.println("***** OfficePopn.subscribeDefault called")
			log.info "***** OfficePopn.subscribeDefault called"
			
			def defaultConsumptionTariff = tariffMarketService.getDefaultTariff(PowerType.CONSUMPTION)
			System.out.println(defaultConsumptionTariff)
			
			sticky++
			def cTS = tariffMarketService.subscribeToTariff(defaultConsumptionTariff, this, (int) customerInfo.population / 10 + sticky)
			System.out.println(cTS)
			this.addToSubscriptions(cTS)
			System.out.println(subscriptions)
			
	//		def defaultProductionTariff = tariffMarketService.getDefaultTariff(PowerType.PRODUCTION)
	//		this.addToSubscriptions(tariffMarketService.subscribeToTariff(defaultProductionTariff, this, customerInfo.population))
			this.save()
		}
		*******************/
		
	//	/**
	//	* Implementation of {@code NewTariffListener} interface.
	//	* Called periodically with a list of newly-published Tariffs.
	//	*/
	//	void publishNewTariffs (List<Tariff> newTariffs) {
	//		System.out.println("OfficePopulation.publishNewTariffs(): newTariffs: " + newTariffs)
	//	}
	
	
	
	
//	/** Subscribing certain subscription */
//	void subscribe(Tariff tariff, int customerCount){
//	}
//  
//	/** Unsubscribing certain subscription */
//	void unsubscribe(TariffSubscription subscription, int customerCount) {
//	}
//  
//	/** Subscribing certain subscription */
//	void addSubscription(TariffSubscription ts) {
//	}
//  
//	/** Unsubscribing certain subscription */
//	void removeSubscription(TariffSubscription ts)	{
//	}
//  
//	//============================= CONSUMPTION - PRODUCTION =================================================
//  
//	/** The first implementation of the power consumption function.
//	 *  I utilized the mean consumption of a neighborhood of households with a random variable */
//	void consumePower() {
//	}
//  
//  
//	double getConsumptionByTimeslot(int serial) {
//		return 0.0
//	}
//  
//	double getConsumptionByTimeslot(TariffSubscription sub) {
//		return 0.0
//	}
//  
//	/** The first implementation of the power consumption function.
//	 *  I utilized the mean consumption of a neighborhood of households with a random variable */
//	void producePower() {
//	}
//  
//	//============================= TARIFF SELECTION PROCESS =================================================
//  
//	/** The first implementation of the changing subscription function.
//	 *  Here we just put the tariff we want to change and the whole population
//	 * is moved to another random tariff.
//	 * @param tariff
//	 */
//	void changeSubscription(Tariff tariff)
//	{
//	}
//  
//	/** In this overloaded implementation of the changing subscription function,
//	 *  Here we just put the tariff we want to change and the whole population
//	 * is moved to another random tariff.
//	 * @param tariff
//	 */
//	void changeSubscription(Tariff tariff, Tariff newTariff)
//	{
//	}
//  
//  
//	/** In this overloaded implementation of the changing subscription function,
//	 * Here we just put the tariff we want to change and amount of the population
//	 * we want to move to the new tariff.
//	 * @param tariff
//	 */
//	void changeSubscription(Tariff tariff, Tariff newTariff, int populationCount)
//	{
//	}
//  
//  
//	/** The first implementation of the tariff selection function.
//	 * This is a random chooser of the available tariffs, totally insensitive.*/
//	Tariff selectTariff(PowerType powerType) {
//	}
//  
//  
//	/** The first implementation of the checking for revoked subscriptions function.*/
//	void checkRevokedSubscriptions(){
//	}
//  
//	void simpleEvaluationNewTariffs(List<Tariff> newTariffs) {
//	}
//  
//	double costEstimation(Tariff tariff)
//	{
//		return 0.0
//	}
//  
//	double estimateFixedTariffPayments(Tariff tariff)
//	{
//		return 0.0
//	}
//  
//	double estimateVariableTariffPayment(Tariff tariff){
//		return 0.0
//	}
//  
//	int logitPossibilityEstimation(Vector estimation) {
//		return 0
// 	}
  
}


