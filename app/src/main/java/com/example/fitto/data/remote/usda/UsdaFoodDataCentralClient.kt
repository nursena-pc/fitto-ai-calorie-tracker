package com.example.fitto.data.remote.usda

import com.example.fitto.data.remote.AndroidRemoteClientLogger
import com.example.fitto.data.remote.NoOpRemoteClientLogger
import com.example.fitto.data.remote.RemoteClientLogger

class UsdaFoodDataCentralClient internal constructor(
    private val transport: UsdaFoodSearchTransport,
    private val parser: UsdaFoodResponseParser,
    private val logger: RemoteClientLogger = NoOpRemoteClientLogger
) : UsdaFoodSearchSource {

    constructor(apiKey: String) : this(
        transport = OkHttpUsdaFoodSearchTransport(apiKey),
        parser = UsdaFoodResponseParser(),
        logger = AndroidRemoteClientLogger
    )

    override fun searchFoods(
        query: String,
        pageSize: Int,
        pageNumber: Int
    ): List<UsdaFoodProduct> {
        val safeQuery = query.trim()
        if (safeQuery.length < MIN_QUERY_LENGTH) {
            logger.debug(TAG, "Query too short: $safeQuery")
            return emptyList()
        }

        val responseBody = transport.search(
            query = safeQuery,
            pageSize = pageSize,
            pageNumber = pageNumber
        ) ?: return emptyList()

        return runCatching {
            parser.parse(responseBody)
        }.onSuccess { products ->
            logger.debug(TAG, "USDA parsed products=${products.size}")
        }.onFailure { throwable ->
            logger.error(TAG, "USDA response parsing failed", throwable)
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val TAG = "UsdaFoodClient"
        const val MIN_QUERY_LENGTH = 3
    }
}
