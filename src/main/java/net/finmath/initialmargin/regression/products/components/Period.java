/*
 * Created on 22.11.2009
 */
package net.finmath.initialmargin.regression.products.components;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;

/**
 * A period. A period has references to the index (coupon) and the notional.
 * It provides the fixing date for the index, the period length, and the payment date.
 *
 * @author Christian Fries
 * @version 1.1
 */
public class Period extends AbstractPeriod {

	private static final long serialVersionUID = -7107623461781510475L;
	private final boolean couponFlow;
	private final boolean notionalFlow;
	private final boolean payer;
	private final boolean isExcludeAccruedInterest;

	/**
	 * Create a simple period with notional and index (coupon) flow.
	 *
	 * @param periodStart              The period start.
	 * @param periodEnd                The period end.
	 * @param fixingDate               The fixing date (as double).
	 * @param paymentDate              The payment date (as double).
	 * @param notional                 The notional object relevant for this period.
	 * @param index                    The index (used for coupon calculation) associated with this period.
	 * @param daycountFraction         The daycount fraction (<code>coupon = index(fixingDate) * daycountFraction</code>).
	 * @param couponFlow               If true, the coupon will be payed. Otherwise there will be not coupon flow.
	 * @param notionalFlow             If true, there will be a positive notional flow at period start (but only if peirodStart &gt; evaluationTime) and a negative notional flow at period end (but only if periodEnd &gt; evaluationTime). Otherwise there will be no notional flows.
	 * @param payer                    If true, the period will be a payer period, i.e. notional and coupon at period end are payed (negative). Otherwise it is a receiver period.
	 * @param isExcludeAccruedInterest If the true, the valuation will exclude accrued interest, if any.
	 */
	public Period(double periodStart, double periodEnd, double fixingDate,
			double paymentDate, AbstractNotional notional, AbstractProductComponent index, double daycountFraction,
			boolean couponFlow, boolean notionalFlow, boolean payer, boolean isExcludeAccruedInterest) {
		super(periodStart, periodEnd, fixingDate, paymentDate, notional, index, daycountFraction);
		this.couponFlow = couponFlow;
		this.notionalFlow = notionalFlow;
		this.payer = payer;
		this.isExcludeAccruedInterest = isExcludeAccruedInterest;
	}

	/**
	 * Create a simple period with notional and index (coupon) flow.
	 * <p>
	 * The valuation does not exclude the accrued interest, i.e., the valuation reports a so called dirty price.
	 *
	 * @param periodStart      The period start.
	 * @param periodEnd        The period end.
	 * @param fixingDate       The fixing date (as double).
	 * @param paymentDate      The payment date (as double).
	 * @param notional         The notional object relevant for this period.
	 * @param index            The index (used for coupon calculation) associated with this period.
	 * @param daycountFraction The daycount fraction (<code>coupon = index(fixingDate) * daycountFraction</code>).
	 * @param couponFlow       If true, the coupon will be payed. Otherwise there will be not coupon flow.
	 * @param notionalFlow     If true, there will be a positive notional flow at period start (but only if peirodStart &gt; evaluationTime) and a negative notional flow at period end (but only if periodEnd &gt; evaluationTime). Otherwise there will be no notional flows.
	 * @param payer            If true, the period will be a payer period, i.e. notional and coupon at period end are payed (negative). Otherwise it is a receiver period.
	 */
	public Period(double periodStart, double periodEnd, double fixingDate,
			double paymentDate, AbstractNotional notional, AbstractProductComponent index, double daycountFraction,
			boolean couponFlow, boolean notionalFlow, boolean payer) {
		this(periodStart, periodEnd, fixingDate, paymentDate, notional, index, daycountFraction, couponFlow, notionalFlow, payer, false);
	}

	/**
	 * Create a simple period with notional and index (coupon) flow.
	 * <p>
	 * The valuation does not exclude the accrued interest, i.e., the valuation reports a so called dirty price.
	 *
	 * @param periodStart  The period start.
	 * @param periodEnd    The period end.
	 * @param fixingDate   The fixing date (as double).
	 * @param paymentDate  The payment date (as double).
	 * @param notional     The notional object relevant for this period.
	 * @param index        The index (coupon) associated with this period.
	 * @param couponFlow   If true, the coupon will be payed. Otherwise there will be not coupon flow.
	 * @param notionalFlow If true, there will be a positive notional flow at period start (but only if peirodStart &gt; evaluationTime) and a negative notional flow at period end (but only if periodEnd &gt; evaluationTime). Otherwise there will be no notional flows.
	 * @param payer        If true, the period will be a payer period, i.e. notional and coupon at period end are payed (negative). Otherwise it is a receiver period.
	 */
	public Period(double periodStart, double periodEnd, double fixingDate,
			double paymentDate, AbstractNotional notional, AbstractProductComponent index,
			boolean couponFlow, boolean notionalFlow, boolean payer) {
		this(periodStart, periodEnd, fixingDate, paymentDate, notional, index, periodEnd - periodStart, couponFlow, notionalFlow, payer);
	}

	/**
	 * This method returns the value random variable of the product within the specified model, evaluated at a given evalutationTime.
	 * Note: For a lattice this is often the value conditional to evalutationTime, for a Monte-Carlo simulation this is the (sum of) value discounted to evaluation time.
	 * Cashflows prior evaluationTime are not considered.
	 *
	 * @param evaluationTime The time on which this products value should be observed.
	 * @param model          The model used to price the product.
	 * @return The random variable representing the value of the product discounted to evaluation time
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	@Override
	public RandomVariable getValue(double evaluationTime, LIBORModelMonteCarloSimulationModel model) throws CalculationException {

		if (evaluationTime >= this.getPaymentDate()) {
			return new RandomVariableFromDoubleArray(0.0);
		}

		// Get random variables
		RandomVariable notionalAtPeriodStart = getNotional().getNotionalAtPeriodStart(this, model);
		RandomVariable numeraireAtEval = model.getNumeraire(evaluationTime);
		RandomVariable numeraire = model.getNumeraire(getPaymentDate());
		// @TODO: Add support for weighted Monte-Carlo.
		//        RandomVariable	monteCarloProbabilities	= model.getMonteCarloWeights(getPaymentDate());

		RandomVariable values;

		// Calculate numeraire relative value of coupon flows
		if (couponFlow) {

			// getCoupon has been changed
			values = getCoupon(evaluationTime, model);   // write here getCoupon(evaluationTime, model); if we want to get future value by going forward on the paths

			values = values.mult(notionalAtPeriodStart);
			values = values.div(numeraire);
			if (isExcludeAccruedInterest && evaluationTime >= getPeriodStart() && evaluationTime < getPeriodEnd()) {
				double nonAccruedInterestRatio = (getPeriodEnd() - evaluationTime) / (getPeriodEnd() - getPeriodStart());
				values = values.mult(nonAccruedInterestRatio);
			}
		} else {
			values = new RandomVariableFromDoubleArray(0.0, 0.0);
		}

		// Apply notional exchange
		if (notionalFlow) {
			RandomVariable nationalAtPeriodEnd = getNotional().getNotionalAtPeriodEnd(this, model);

			if (getPeriodStart() > evaluationTime) {
				RandomVariable numeraireAtPeriodStart = model.getNumeraire(getPeriodStart());
				values = values.subRatio(notionalAtPeriodStart, numeraireAtPeriodStart);
			}

			if (getPeriodEnd() > evaluationTime) {
				RandomVariable numeraireAtPeriodEnd = model.getNumeraire(getPeriodEnd());
				values = values.addRatio(nationalAtPeriodEnd, numeraireAtPeriodEnd);
			}
		}

		if (payer) {
			values = values.mult(-1.0);
		}

		values = values.mult(numeraireAtEval);

		// Return values
		return values;
	}

	@Override
	public RandomVariable getValue(double evaluationTime, double fixingTime, LIBORModelMonteCarloSimulationModel model) throws CalculationException {

		return getValue(evaluationTime, model);
	}

	@Override
	public RandomVariable getCoupon(LIBORModelMonteCarloSimulationModel model) throws CalculationException {
		// Calculate percentage value of coupon (not multiplied with notional, not discounted)
		RandomVariable values = getIndex().getValue(getFixingDate(), model);

		// Apply daycount fraction
		double periodDaycountFraction = getDaycountFraction();
		values = values.mult(periodDaycountFraction);

		return values; // hitherto not discounted
	}

	//INSERTED: We use this to get the future value without conditional expectation
	public RandomVariable getCoupon(double evaluationTime, LIBORModelMonteCarloSimulationModel model) throws CalculationException {
		// Calculate percentage value of coupon (not multiplied with notional, not discounted)
		RandomVariable values = getIndex().getValue(evaluationTime, getFixingDate(), model);

		// Apply daycount fraction
		double periodDaycountFraction = getDaycountFraction();
		values = values.mult(periodDaycountFraction);

		return values; // hitherto not discounted
	}

	@Override
	public String toString() {
		return "Period [couponFlow=" + couponFlow + ", notionalFlow="
				+ notionalFlow + ", payer=" + payer + ", toString()="
				+ super.toString() + "]";
	}

	@Override
	public RandomVariable getCF(double initialTime, double finalTime, LIBORModelMonteCarloSimulationModel model) throws CalculationException {
		RandomVariable values = new RandomVariableFromDoubleArray(0.0);
		if (initialTime >= this.getPaymentDate() || finalTime < this.getPaymentDate()) {
			// Apply notional exchange
			if (notionalFlow && finalTime >= getPeriodEnd()) {
				RandomVariable notionalAtPeriodEnd = getNotional().getNotionalAtPeriodEnd(this, model);
				RandomVariable numeraireAtPeriodEnd = model.getNumeraire(getPeriodEnd());
				values = values.addRatio(notionalAtPeriodEnd, numeraireAtPeriodEnd);
			} else {
				return values;
			}
		} else {

			// Get random variables
			RandomVariable notionalAtPeriodStart = getNotional().getNotionalAtPeriodStart(this, model);
			RandomVariable numeraire = model.getNumeraire(getPaymentDate());
			// @TODO: Add support for weighted Monte-Carlo.
			//        RandomVariable	monteCarloProbabilities	= model.getMonteCarloWeights(getPaymentDate());

			// Calculate numeraire relative value of coupon flows
			if (couponFlow) {
				values = getCoupon(finalTime, model); //not discounted
				values = values.mult(notionalAtPeriodStart);
				values = values.div(numeraire);
				if (isExcludeAccruedInterest && finalTime >= getPeriodStart() && finalTime < getPeriodEnd()) {
					double nonAccruedInterestRatio = (getPeriodEnd() - finalTime) / (getPeriodEnd() - getPeriodStart());
					values = values.mult(nonAccruedInterestRatio);
				}
			} else {
				values = new RandomVariableFromDoubleArray(0.0, 0.0);
			}

			// Apply notional exchange
			if (notionalFlow && finalTime >= getPeriodEnd()) {
				RandomVariable notionalAtPeriodEnd = getNotional().getNotionalAtPeriodEnd(this, model);
				RandomVariable numeraireAtPeriodEnd = model.getNumeraire(getPeriodEnd());
				values = values.addRatio(notionalAtPeriodEnd, numeraireAtPeriodEnd);
			}
		}

		if (payer) {
			values = values.mult(-1.0);
		}
		RandomVariable numeraireAtEval = model.getNumeraire(initialTime); // was finalTime
		values = values.mult(numeraireAtEval);

		// Return values
		return values;
	}
}
