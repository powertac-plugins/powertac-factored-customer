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

import org.apache.commons.math.distribution.*


/**
 * @author Prashant
 *
 */
class ProbabilityDistribution
{
	enum DistributionType { DEGENERATE, POINTMASS, UNIFORM, NORMAL, GAUSSIAN,  LOGNORMAL, Z, STDNORMAL, INTERVAL }
			     //	CAUCHY, BETA, BINOMIAL, POISSON, CHISQUARED, EXPONENTIAL, GAMMA, WEIBULL, T, STUDENT, F, SNEDECOR, }
	
	DistributionType distributionType	
	long seed
	DistributionSamplerBase sampler
	
	static constraints = {
		distributionType(nullable: false)
		sampler(nullable: false)
	}	
	
	/**
	 * @param xml - XML GPathResult from config
	 */
	ProbabilityDistribution init(def xml, long randomSeed)
	{
		seed = randomSeed
		try {
			distributionType = Enum.valueOf(DistributionType.class, xml.@probability.text())
		} catch (IllegalArgumentException e) {
			throw new Error("Unsupported probability distribution type: ${distributionType}")
		}
		switch (distributionType) {
			case [DistributionType.POINTMASS, DistributionType.DEGENERATE]:
				def param1 = xml.@value.text().toDouble()
				sampler = new DistributionSamplerDegenerate(value: param1)
				break
			case DistributionType.UNIFORM:
				def param1 = xml.@low.text().toDouble()
				def param2 = xml.@high.text().toDouble()
				sampler = new DistributionSamplerUniform(low: param1, high: param2)
				break
			case [DistributionType.NORMAL, DistributionType.GAUSSIAN]:
				def param1 = xml.@mean.text().toDouble()
				def param2 = xml.@stdDev.text().toDouble()
				sampler = new DistributionSamplerNormal(mean: param1, stdDev: param2)
				break
			case DistributionType.LOGNORMAL:
				def param1 = Math.log(xml.@expMean.text().toDouble())
				def param2 = Math.log(xml.@expStdDev.text().toDouble())
				sampler = new DistributionSamplerNormal(mean: param1, stdDev: param2)
				break
			case [DistributionType.Z, DistributionType.STDNORMAL]:
				sampler = new DistributionSamplerNormal(mean: 0, stdDev: 1)
				break
			case [DistributionType.Z, DistributionType.INTERVAL]:
				def param1 = xml.@mean.text().toDouble()
				def param2 = xml.@stdDev.text().toDouble()
				def param3 = xml.@low.text().toDouble()
				def param4 = xml.@high.text().toDouble() 
				sampler = new DistributionSamplerInterval(mean: param1, stdDev: param2, low: param3, high: param4)
				break
			/**
			case DistributionType.CAUCHY:
				param1 = xml.@median.text().toDouble()
				param2 = xml.@scale.text().toDouble()
				sampler = new CauchyDistributionImpl(param1, param2)
				break
			case DistributionType.BETA:
				param1 = xml.@alpha.text().toDouble()
				param2 = xml.@beta.text().toDouble()
				sampler = new BetaDistributionImpl(param1)
				break
			case DistributionType.BINOMIAL:
				param1 = xml.@trials.text().toInteger()
				param2 = xml.@success.text().toDouble()
				sampler = new BinomialDistributionImpl(param1, param2)
				break
			case DistributionType.POISSON:
				param1 = xml.@lambda.text().toDouble()
				sampler = new PoissonDistributionImpl(param1)
				break
			case DistributionType.CHISQUARED:
				param1 = xml.@dof.text().toDouble()
				sampler = new ChiSquaredDistributionImpl(param1)
				break
			case DistributionType.EXPONENTIAL:
				param1 = xml.@mean.text().toDouble()
				sampler = new ExponentialDistributionImpl(param1)
				break
			case DistributionType.GAMMA:
				param1 = xml.@alpha.text().toDouble()
				param2 = xml.@beta.text().toDouble()
				sampler = new BetaDistributionImpl(param1)
				break
			case DistributionType.WEIBULL:
				param1 = xml.@alpha.text().toDouble()
				param2 = xml.@beta.text().toDouble()
				sampler = new WeibullDistributionImpl(param1, param2)
				break
			case [DistributionType.T, DistributionType.STUDENT]:
				param1 = xml.@dof.text().toDouble()
				sampler = new TDistributionImpl(param1, param2)
				break
			case [DistributionType.F, DistributionType.SNEDECOR]:
				param1 = xml.@d1.text().toDouble()
				param2 = xml.@d2.text().toDouble()
				sampler = new FDistributionImpl(param1, param2)
				break
			**/
			default:  throw new Error("Shouldn't be getting to unknown distributionType: ${distributionType}!")
		}
		sampler.init(seed)
		assert(sampler.save())
		assert(this.save())
		log.debug "init(xml, seed): " + toString() + " sampler: ${sampler}"
		return this
	}
	
	double drawSample()
	{
		switch (distributionType) {
			case null:
				throw new Error("ProbabilityDistribution.drawSample(): distributionType should not be null!")
			case DistributionType.LOGNORMAL:
				return Math.exp(sampler.sample())	
			default:
				return sampler.sample()
		}
	}
	
	String toString()
	{
		return "ProbabilityDistribution(distributionType: ${distributionType}, seed: ${seed})"
	}

} // end class

