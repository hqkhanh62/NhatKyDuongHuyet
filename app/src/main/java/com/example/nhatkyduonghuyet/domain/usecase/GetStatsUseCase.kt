package com.example.nhatkyduonghuyet.domain.usecase

import com.example.nhatkyduonghuyet.domain.repository.LogRepository
import com.example.nhatkyduonghuyet.data.model.AdvancedStatsEntity
import kotlinx.coroutines.flow.Flow

class GetStatsUseCase(
    private val repo: LogRepository
) {
    operator fun invoke(): Flow<AdvancedStatsEntity> {
        return repo.getAdvancedStats()
    }
}
