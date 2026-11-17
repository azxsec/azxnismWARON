package com.azxnism.waron.data.repository

import com.azxnism.waron.data.local.ScanDao
import com.azxnism.waron.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanRepository @Inject constructor(
    private val scanDao: ScanDao
) {
    fun getAllScans(): Flow<List<ScanRecord>> = scanDao.getAllScans()

    fun getRecentScans(limit: Int = 5): Flow<List<ScanRecord>> = scanDao.getRecentScans(limit)

    suspend fun getScanById(scanId: Long): ScanRecord? = scanDao.getScanById(scanId)

    fun observeScan(scanId: Long): Flow<ScanRecord?> = scanDao.observeScan(scanId)

    suspend fun createScan(targetUrl: String, modules: List<String>): Long {
        val scan = ScanRecord(
            targetUrl = targetUrl,
            enabledModules = modules,
            status = ScanStatus.QUEUED
        )
        return scanDao.insertScan(scan)
    }

    suspend fun updateScan(scan: ScanRecord) = scanDao.updateScan(scan)

    suspend fun deleteScan(scan: ScanRecord) {
        scanDao.deleteFindingsForScan(scan.id)
        scanDao.deleteScan(scan)
    }

    suspend fun insertFinding(finding: Finding): Long = scanDao.insertFinding(finding)

    fun getFindingsForScan(scanId: Long): Flow<List<Finding>> =
        scanDao.getFindingsForScan(scanId)

    suspend fun getFindingsForScanDirect(scanId: Long): List<Finding> =
        scanDao.getFindingsForScanDirect(scanId)

    fun getDashboardStats(): Flow<DashboardStats> {
        return combine(
            scanDao.getTotalScanCount(),
            scanDao.getTotalFindingCount(),
            scanDao.getFindingCountBySeverity(Severity.CRITICAL),
            scanDao.getFindingCountBySeverity(Severity.HIGH),
            scanDao.getFindingCountBySeverity(Severity.MEDIUM),
            scanDao.getFindingCountBySeverity(Severity.LOW)
        ) { values ->
            DashboardStats(
                totalScans = values[0] as Int,
                totalFindings = values[1] as Int,
                criticalFindings = values[2] as Int,
                highFindings = values[3] as Int,
                mediumFindings = values[4] as Int,
                lowFindings = values[5] as Int
            )
        }
    }

    suspend fun getScanWithFindings(scanId: Long): ScanWithFindings? {
        val scan = scanDao.getScanById(scanId) ?: return null
        val findings = scanDao.getFindingsForScanDirect(scanId)
        return ScanWithFindings(scan, findings)
    }
}
