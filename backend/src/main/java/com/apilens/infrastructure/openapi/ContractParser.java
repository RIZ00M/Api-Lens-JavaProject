package com.apilens.infrastructure.openapi;

import com.apilens.domain.model.Api;
import com.apilens.domain.model.ApiContract;

import java.util.List;

/**
 * Converts raw specification bytes into API Lens's own normalized
 * ApiContract model. Deliberately an interface (product spec section 22,
 * "Contract parser: ContractParser, implementations OpenApi3Parser,
 * Swagger2Parser") so the specific parsing library stays swappable and
 * isolated to infrastructure -- nothing outside this package touches the
 * underlying OpenAPI object model.
 */
public interface ContractParser {

    ParseResult parse(Api api, String sourceUrl, byte[] rawContent);

    /**
     * @param contract fully-built (but not yet persisted) contract graph, present iff successful
     * @param errors   human-readable parse problems; non-empty when contract is null,
     *                 may be non-empty (warnings) even when contract is present
     */
    record ParseResult(boolean success, ApiContract contract, List<String> errors) {

        public static ParseResult failure(List<String> errors) {
            return new ParseResult(false, null, errors);
        }

        public static ParseResult success(ApiContract contract, List<String> warnings) {
            return new ParseResult(true, contract, warnings);
        }
    }
}
