package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM custom_products ORDER BY id DESC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM custom_products WHERE type = :type OR type = 'ALL' ORDER BY id DESC")
    fun getProductsByType(type: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM custom_products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): ProductEntity?

    @Query("SELECT COUNT(*) FROM custom_products")
    suspend fun getProductCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("DELETE FROM custom_products WHERE id = :id")
    suspend fun deleteProductById(id: Long)

    @Query("DELETE FROM custom_products")
    suspend fun deleteAllProducts()

    @Query("DELETE FROM custom_products WHERE name IN (:names)")
    suspend fun deleteProductsByNames(names: List<String>)
}
