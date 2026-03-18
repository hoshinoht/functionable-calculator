package edu.singaporetech.inf2007quiz01.data

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface MathJsApi {
    @GET(".")
    suspend fun calculate(@Query("expr") expr: String): Response<ResponseBody>
}
