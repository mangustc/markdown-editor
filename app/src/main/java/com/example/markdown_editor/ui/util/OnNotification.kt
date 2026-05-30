package com.example.markdown_editor.ui.util

import android.content.res.Resources
import com.example.markdown_editor.R
import com.example.markdown_editor.domain.exceptions.FileNotFoundException
import com.example.markdown_editor.domain.exceptions.FileNotReadableException
import com.example.markdown_editor.domain.exceptions.FileNotWritableException
import com.example.markdown_editor.domain.exceptions.LinkFetchException
import com.example.markdown_editor.domain.exceptions.LinkPreviewException
import com.example.markdown_editor.domain.exceptions.ProjectAccessException
import com.example.markdown_editor.domain.exceptions.ProjectException
import com.example.markdown_editor.domain.exceptions.SyncAuthException
import com.example.markdown_editor.domain.exceptions.SyncException
import com.example.markdown_editor.domain.exceptions.SyncNetworkException
import com.example.markdown_editor.domain.exceptions.SyncQuotaException
import com.example.markdown_editor.domain.exceptions.SyncServerException
import com.example.markdown_editor.domain.exceptions.SyncStateException
import com.example.markdown_editor.ui.viewmodel.events.NotificationEvent

fun onNotificationToast(event: NotificationEvent, toast: (String) -> Unit, resources: Resources) {
    when (event) {
        is NotificationEvent.LinkCopied -> toast(resources.getString(R.string.link_copied))
        is NotificationEvent.FailedToAddPhoto -> toast(resources.getString(R.string.failed_to_create_photo_container))
        is NotificationEvent.FailedToStartCamera -> toast(resources.getString(R.string.failed_to_start_camera))
        is NotificationEvent.NoAppFoundToOpenThisFile -> toast(resources.getString(R.string.no_app_found_to_open_this_file))
        is NotificationEvent.SyncServiceIsNone -> toast(resources.getString(R.string.no_sync_service_configured_configure_one_in_settings))
        is NotificationEvent.CustomMessage -> toast(event.message)
        is NotificationEvent.FromException -> when (val e = event.exception) {
            is SyncException -> when (e) {
                is SyncAuthException -> toast(resources.getString(R.string.authentication_failed_please_log_in_again))
                is SyncNetworkException -> toast(resources.getString(R.string.network_error_check_internet_connection))
                is SyncQuotaException -> toast(resources.getString(R.string.cloud_storage_full_free_up_space))
                is SyncServerException -> toast(resources.getString(R.string.sync_server_unavailable_try_again_later))
                is SyncStateException -> toast(resources.getString(R.string.sync_data_corrupted_please_reset_sync))
            }

            is ProjectException -> when (e) {
                is FileNotFoundException -> toast(
                    resources.getString(
                        R.string.file_not_found,
                        e.path,
                    ),
                )

                is FileNotReadableException -> toast(
                    resources.getString(
                        R.string.file_not_readable,
                        e.path,
                    ),
                )

                is FileNotWritableException -> toast(
                    resources.getString(
                        R.string.file_not_writable,
                        e.path,
                    ),
                )

                is ProjectAccessException -> toast(
                    resources.getString(
                        R.string.failed_to_access_project_directory,
                        e.path,
                    ),
                )
            }

            is LinkPreviewException -> when (e) {
                is LinkFetchException -> toast(
                    resources.getString(
                        R.string.failed_to_fetch_link_information,
                        e.path,
                    ),
                )
            }

            else -> toast(
                e.message ?: resources.getString(
                    R.string.unknown_error,
                    e.localizedMessage ?: "Unknown",
                ),
            )
        }
    }
}
