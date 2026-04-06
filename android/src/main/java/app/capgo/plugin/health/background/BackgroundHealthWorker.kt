package app.capgo.plugin.health.background

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class BackgroundHealthWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    private val coordinator = BackgroundHealthCoordinator(appContext)

    override suspend fun doWork(): Result {
        return coordinator.run()
    }
}
