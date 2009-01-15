/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jing.rxn;

import java.util.HashMap;
import java.util.Iterator;
import jing.chem.LennardJones;
import jing.chem.Species;
import jing.rxnSys.ReactionSystem;

/**
 * Represents the bath gas. Calculates parameters of the bath gas as a
 * weighted average of the components in the bath gas, where the weights
 * are the mole fractions.
 * @author jwallen
 */
public class BathGas {
	private double expDownParam = 0.0;
	private double ljSigma = 0.0;
	private double ljEpsilon = 0.0;
	private double molWt = 0.0;
	private HashMap colliders;

	public BathGas() {
		colliders = null;
	}

	public BathGas(ReactionSystem rxnSystem) {
		colliders = rxnSystem.identifyColliders();
		update();
	}

	public double getExpDownParam() {
		return expDownParam;
	}

	public double getLJSigma() {
		return ljSigma;
	}

	public double getLJEpsilon() {
		return ljEpsilon;
	}

	public double getMolecularWeight() {
		return molWt;
	}

	public HashMap getColliders() {
		return colliders;
	}

	public void setColliders(ReactionSystem rxnSystem) {
		colliders = rxnSystem.identifyColliders();
		update();
	}

	/**
	 * Updates the bath gas parameters via a weighted average.
	 */
	private void update() {

		// Check for null pointer (i.e. colliders not set)
		if (colliders == null)
			throw new NullPointerException();

		// Clear parameters
		expDownParam = 0.0;
		ljSigma = 0.0;
		ljEpsilon = 0.0;
		molWt = 0.0;

		// Determine bath gas concentration (i.e. total concentration of colliders)
		double totalConc = 0;
		for (Iterator iter = colliders.values().iterator(); iter.hasNext();) {
			totalConc += ((Double) iter.next()).doubleValue();
		}

		// Calculate values as weighted average
		for (Iterator iter = colliders.keySet().iterator(); iter.hasNext();) {

			Object key = iter.next();
			if (key instanceof Species) {
				Species spe = (Species) key;
				double conc = ((Double) colliders.get(spe)).doubleValue();
				double mf = conc/totalConc;

				molWt += mf * spe.getMolecularWeight();
				expDownParam += mf * spe.getDeltaEDown();
				ljSigma += mf * spe.getLJ().getSigma();
				ljEpsilon += mf * spe.getLJ().getEpsilon();
			}
			else if (key instanceof String) {
				String name = (String) key;
				LennardJones lj = new LennardJones();
				ljSigma = lj.getSigma();
				ljEpsilon = lj.getSigma();

				if (name.equals("Ar") || name.equals("AR")) {
					expDownParam = 374.0;
					molWt = 39.95;
				}
				else if (name.equals("N2")) {
					expDownParam = 461.0;
					molWt = 28.01;
				}
				else if (name.equals("He") || name.equals("HE")) {
					expDownParam = 291.0;
					molWt = 4.00;
				}
				else {
					System.out.println("unknown colliders: " + name);
					System.exit(0);
				}
			}
			else {
				System.out.println("unknown colliders: " + key.toString());
				System.exit(0);
			}
		}

		// Convert to units used by FAME
		expDownParam *= 2.9979e10 * 6.626e-34 * 6.022e23 / 1000; // cm^-1 --> kJ/mol
		ljSigma *= 1e-10; // A --> m
		ljEpsilon *= 1.381e-23; // K --> J

	}
}
