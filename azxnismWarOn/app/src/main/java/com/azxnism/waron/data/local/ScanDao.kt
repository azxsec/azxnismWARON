package com.azxnism.waron.data.local

import androidx.room.*
import com.azxnism.waron.data.model.Finding
import com.azxnism.waron.data.model.ScanRecord
import com.azxnism.waron.data.model.ScanStatus
import com.azxnism.waron.data.model.Severity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {

    // Scan operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanRecord): Long

    @Update
    suspend fun updateScan(scan: ScanRecord)

    @Delete
    suspend fun deleteScan(scan: ScanRecord)

    @Query("SELECT * FROM scans ORDER BY startedAt DESC")
    fun getAllScans(): Flow<List<ScanRecord>>

    @Query("SELECT * FROM scans WHERE id = :scanId")
    suspend fun getScanById(scanId: Long): ScanRecord?

    @Query("SELECT * FROM scans WHERE id = :scanId")
    fun observeScan(scanId: Long): Flow<ScanRecord?>

    @Query("SELECT * FROM scans ORDER BY startedAt DESC LIMIT :limit")
    fun getRecentScans(limit: Int = 5): Flow<List<ScanRecord>>

    @Query("SELECT COUNT(*) FROM scans")
    fun getTotalScanCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM scans WHERE status = :status")
    fun getScanCountByStatus(status: ScanStatus): Flow<Int>

    // Finding operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinding(finding: Finding): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFindings(findings: List<Finding>)

    @Query("SELECT * FROM findings WHERE scanId = :scanId ORDER BY severity ASC")
    fun getFindingsForScan(scanId: Long): Flow<List<Finding>>

    @Query("SELECT * FROM findings WHERE scanId = :scanId ORDER BY severity ASC")
    suspend fun getFindingsForScanDirect(scanId: Long): List<Finding>

    @Query("SELECT COUNT(*) FROM findings")
    fun getTotalFindingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM findings WHERE severity = :severity")
    fun getFindingCountBySeverity(severity: Severity): Flow<Int>

    @Query("DELETE FROM findings WHERE scanId = :scanId")
    suspend fun deleteFindingsForScan(scanId: Long)

    // Dashboard stats
    @Query("""
        SELECT COUNT(*) FROM findings 
        WHERE severity = 'CRITICAL' OR severity = 'HIGH'
    """)
    fun getCriticalAndHighCount(): Flow<Int>
}
