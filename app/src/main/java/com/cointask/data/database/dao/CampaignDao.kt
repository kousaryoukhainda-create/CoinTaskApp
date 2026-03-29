package com.cointask.data.database.dao

import androidx.room.*
import com.cointask.data.models.Campaign
import com.cointask.data.models.CampaignStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CampaignDao {
    @Query("SELECT * FROM campaigns WHERE advertiserId = :advertiserId")
    fun getCampaignsByAdvertiser(advertiserId: Int): Flow<List<Campaign>>

    @Insert
    suspend fun insertCampaign(campaign: Campaign): Long

    @Update
    suspend fun updateCampaign(campaign: Campaign)

    @Query("UPDATE campaigns SET spentAmount = spentAmount + :amount, completedTasks = completedTasks + 1 WHERE id = :campaignId")
    suspend fun updateCampaignSpending(campaignId: Int, amount: Int)

    @Query("SELECT * FROM campaigns")
    suspend fun getAllCampaignsList(): List<Campaign>

    @Delete
    suspend fun deleteCampaign(campaign: Campaign)
}
