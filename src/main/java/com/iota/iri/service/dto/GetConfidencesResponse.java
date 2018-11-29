package com.iota.iri.service.dto;

import com.iota.iri.service.API;
import java.util.List;

/**
 * 
 * Contains information about the result of a successful {@code getConfidences} API call.
 * See {@link API#getConfidencesStatement} for how this response is created.
 *
 */
public class GetConfidencesResponse extends AbstractResponse {

    /**
     * A list of floating point values in the [0,1] interval, representing confirmation confidences
     */
	private Double [] confidences; 

	/**
     * Creates a new {@link GetConfidencesResponse}
     * 
     * @param confidences {@link #states}
     * @return an {@link GetConfidencesResponse} filled with the error message
     */
	public static AbstractResponse create(List<Double> confidences) {
		GetConfidencesResponse res = new GetConfidencesResponse();
		res.confidences = confidences.toArray(new Double[confidences.size()]);
		return res;
	}
	
    /**
     * 
     * @return {@link #states}
     */
	public Double [] getConfidences() {
		return confidences;
	}

}
