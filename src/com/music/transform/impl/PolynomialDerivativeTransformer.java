package com.music.transform.impl;

import com.music.domain.Cadence;
import com.music.transform.Transformer;

import java.util.Arrays;

/**
 * Polynomial Derivative – fits a polynomial to each chord,
 * computes its symbolic derivative, and evaluates it at each original index.
 */
public class PolynomialDerivativeTransformer implements Transformer {
    @Override
    public Cadence transform(Cadence c) {
        int[][] input = c.intervals();
        int[][] output = new int[input.length][];

        for (int i = 0; i < input.length; i++) {
            int[] y = input[i];
            int n = y.length;
            double[] x = new double[n];
            for (int j = 0; j < n; j++) x[j] = j;

            // Fit polynomial of degree n−1
            double[] coeffs = fitPolynomial(x, y);

            // Derive polynomial
            double[] deriv = new double[coeffs.length - 1];
            for (int j = 1; j < coeffs.length; j++) {
                deriv[j - 1] = coeffs[j] * j;
            }

            // Evaluate derivative at original x
            output[i] = new int[n];
            for (int j = 0; j < n; j++) {
                output[i][j] = (int) Math.round(evaluatePolynomial(deriv, x[j]));
            }
        }

        return new Cadence(
            "Polynomial Derivative of " + c.type(),
            output,
            null,
            "Symbolic derivative of fitted polynomial"
        );
    }

    private double[] fitPolynomial(double[] x, int[] y) {
        int n = x.length;
        double[][] matrix = new double[n][n];
        double[] rhs = new double[n];

        for (int i = 0; i < n; i++) {
            double xi = 1;
            for (int j = 0; j < n; j++) {
                matrix[i][j] = xi;
                xi *= x[i];
            }
            rhs[i] = y[i];
        }

        return solve(matrix, rhs);
    }

    private double evaluatePolynomial(double[] coeffs, double x) {
        double result = 0;
        double xi = 1;
        for (double c : coeffs) {
            result += c * xi;
            xi *= x;
        }
        return result;
    }

    private double[] solve(double[][] A, double[] b) {
        int n = b.length;
        double[] x = Arrays.copyOf(b, n);
        for (int i = 0; i < n; i++) {
            double pivot = A[i][i];
            for (int j = i; j < n; j++) A[i][j] /= pivot;
            x[i] /= pivot;
            for (int k = i + 1; k < n; k++) {
                double factor = A[k][i];
                for (int j = i; j < n; j++) A[k][j] -= factor * A[i][j];
                x[k] -= factor * x[i];
            }
        }
        for (int i = n - 1; i >= 0; i--) {
            for (int j = i + 1; j < n; j++) x[i] -= A[i][j] * x[j];
        }
        return x;
    }
}
