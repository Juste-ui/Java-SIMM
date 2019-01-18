/*
 * Created on 05.12.2009
 *
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 */
package net.finmath.initialmargin.regression.products.components;

import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;

/**
 * A constant (non-stochastic) notional.
 *
 * @author Christian Fries
 */
public class Notional implements AbstractNotional {

	private final String currency;
	private final RandomVariableFromDoubleArray notional;

	/**
	 * Creates a constant (non-stochastic) notional.
	 *
	 * @param notional The constant notional value.
	 * @param currency The currency.
	 */
	public Notional(double notional, String currency) {
		super();
		this.notional = new RandomVariableFromDoubleArray(0.0, notional);
		this.currency = currency;
	}

	/**
	 * Creates a constant (non-stochastic) notional.
	 *
	 * @param notional The constant notional value.
	 */
	public Notional(double notional) {
		this(notional, null);
	}

	@Override
	public String getCurrency() {
		return currency;
	}

	@Override
	public RandomVariable getNotionalAtPeriodEnd(AbstractPeriod period, LIBORModelMonteCarloSimulationModel model) {
		return notional;
	}

	@Override
	public RandomVariable getNotionalAtPeriodStart(AbstractPeriod period, LIBORModelMonteCarloSimulationModel model) {
		return notional;
	}

	@Override
	public String toString() {
		return "Notional [currency=" + currency + ", notional=" + notional
				+ "]";
	}
}
