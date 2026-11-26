package de.kinderalltag.ui

import de.kinderalltag.R
import de.kinderalltag.text.*

internal val TextKey.resourceId: Int
    get() =
        when (this) {
            TextKey.DEFAULT_APPOINTMENT_DURATION -> R.string.msg_default_appointment_duration
            TextKey.APPOINTMENT_DURATION_RANGE -> R.string.msg_appointment_duration_range
            TextKey.APPOINTMENT_DURATION_MINUTES -> R.string.msg_appointment_duration_minutes
            TextKey.APPOINTMENT_DURATION_HELP -> R.string.msg_appointment_duration_help
            TextKey.APPOINTMENT_DURATION_CLIPPED -> R.string.msg_appointment_duration_clipped

            TextKey.CHILD_SETUP_FAILED -> R.string.msg_child_setup_failed
            TextKey.CHILD_NAME_REQUIRED -> R.string.msg_child_name_required
            TextKey.CHILD_CHANGED_RELOAD -> R.string.msg_child_changed_reload
            TextKey.TEN_ACTIVE_CHILDREN -> R.string.msg_ten_active_children
            TextKey.CHILDREN_HELP -> R.string.msg_children_help
            TextKey.CHILD_ARCHIVED -> R.string.msg_child_archived
            TextKey.CHILD_ARCHIVE -> R.string.msg_child_archive
            TextKey.CHILD_RESTORE -> R.string.msg_child_restore
            TextKey.CHILD_ARCHIVE_HELP -> R.string.msg_child_archive_help
            TextKey.CHILD_RESTORE_HELP -> R.string.msg_child_restore_help
            TextKey.ADD_CHILD -> R.string.msg_add_child
            TextKey.ADD_CHILD_FIRST -> R.string.msg_add_child_first
            TextKey.CHILD_ICON -> R.string.msg_child_icon
            TextKey.CHILD_COLOR -> R.string.msg_child_color
            TextKey.ALL_CHILDREN -> R.string.msg_all_children
            TextKey.CONFIRM -> R.string.msg_confirm
            TextKey.CHILD_STATISTICS_HELP -> R.string.msg_child_statistics_help
            TextKey.CHILD_ICON_BEAR -> R.string.msg_child_icon_bear
            TextKey.CHILD_ICON_FOX -> R.string.msg_child_icon_fox
            TextKey.CHILD_ICON_RABBIT -> R.string.msg_child_icon_rabbit
            TextKey.CHILD_ICON_LION -> R.string.msg_child_icon_lion
            TextKey.CHILD_ICON_ELEPHANT -> R.string.msg_child_icon_elephant
            TextKey.CHILD_ICON_WHALE -> R.string.msg_child_icon_whale
            TextKey.CHILD_ICON_PENGUIN -> R.string.msg_child_icon_penguin
            TextKey.CHILD_ICON_DINOSAUR -> R.string.msg_child_icon_dinosaur
            TextKey.CHILD_ICON_TRACTOR -> R.string.msg_child_icon_tractor
            TextKey.CHILD_ICON_UNICORN -> R.string.msg_child_icon_unicorn
            TextKey.CHILD_COLOR_TEAL -> R.string.msg_child_color_teal
            TextKey.CHILD_COLOR_ORANGE -> R.string.msg_child_color_orange
            TextKey.CHILD_COLOR_CORAL -> R.string.msg_child_color_coral
            TextKey.CHILD_COLOR_INDIGO -> R.string.msg_child_color_indigo

            TextKey.DELETED -> R.string.msg_deleted
            TextKey.CHANGED -> R.string.msg_changed
            TextKey.RECORDED_BY -> R.string.msg_recorded_by
            TextKey.TO_ADD -> R.string.msg_to_add
            TextKey.SHOPPING_ITEMS_QUEUED_FOR_SYNCHRONIZATION ->
                R.string.msg_shopping_items_queued_for_synchronization
            TextKey.MINUTES_BEFORE -> R.string.msg_minutes_before
            TextKey.DAYS -> R.string.msg_days
            TextKey.DAYS_FORWARD -> R.string.msg_days_forward
            TextKey.DAYS_BACK -> R.string.msg_days_back
            TextKey.DUPLICATE_OR_ALREADY_OPEN_ITEMS_WILL_BE_SKIPPED ->
                R.string.msg_duplicate_or_already_open_items_will_be_skipped
            TextKey.APPEARS_MORE_THAN_ONCE -> R.string.msg_appears_more_than_once
            TextKey.DELETE_CONFIRMATIONS_AND_HISTORY_ARE_PRESERVED_YOU_CAN_UNDO_THE_DELETION ->
                R.string
                    .msg_delete_confirmations_and_history_are_preserved_you_can_undo_the_deletion
            TextKey.NEW_SHOPPING_ITEMS_UP_TO_PER_IMPORT_YOU_CAN_EDIT_MERGE_OR_REMOVE_LINES ->
                R.string.msg_new_shopping_items_up_to_per_import_you_can_edit_merge_or_remove_lines
            TextKey.OF_TASKS_COMPLETED -> R.string.msg_of_tasks_completed
            TextKey.OF_APPOINTMENTS_FINISHED -> R.string.msg_of_appointments_finished
            TextKey.RECORDED_BY_8B6641 -> R.string.msg_recorded_by_8b6641
            TextKey.OF -> R.string.msg_of
            TextKey.MORE_ALL_ENTRIES_ARE_LISTED_BELOW ->
                R.string.msg_more_all_entries_are_listed_below
            TextKey.VALUE_1_TO_365_DAYS_AFTER_THE_DISPLAYED_DAY_DEFAULT_7 ->
                R.string.msg_value_1_to_365_days_after_the_displayed_day_default_7
            TextKey.VALUE_1_TO_365_DAYS_DEFAULT_7 -> R.string.msg_value_1_to_365_days_default_7
            TextKey.VALUE_1_TO_50_ON_THIS_DEVICE -> R.string.msg_value_1_to_50_on_this_device
            TextKey.VALUE_90_DAYS -> R.string.msg_value_90_days
            TextKey.VALUE_90_DAYS_FORWARD -> R.string.msg_value_90_days_forward
            TextKey.VALUE_90_DAYS_BACK -> R.string.msg_value_90_days_back
            TextKey.NO_OCCURRENCE -> R.string.msg_no_occurrence
            TextKey.FROM -> R.string.msg_from
            TextKey.THERE_ARE_NO_OCCURRENCES_FROM_THIS_DATE_ONWARD ->
                R.string.msg_there_are_no_occurrences_from_this_date_onward
            TextKey.SHOW_FROM_TODAY -> R.string.msg_show_from_today
            TextKey.FROM_ORIGINAL_APPOINTMENT -> R.string.msg_from_original_appointment
            TextKey.CANCEL -> R.string.msg_cancel
            TextKey.CURRENT_SHOPPING_LIST -> R.string.msg_current_shopping_list
            TextKey.ALL -> R.string.msg_all
            TextKey.EVERY -> R.string.msg_every
            TextKey.EVERY_MONTHS -> R.string.msg_every_months
            TextKey.EVERY_DAYS -> R.string.msg_every_days
            TextKey.EVERY_WEEKS -> R.string.msg_every_weeks
            TextKey.SHOW_ALL_TASKS -> R.string.msg_show_all_tasks
            TextKey.SHOW_ALL_LABELS -> R.string.msg_show_all_labels
            TextKey.RETRACT_ALL_CURRENT_CONFIRMATIONS_FOR_THIS_OCCURRENCE_THE_CORRECTION_REM ->
                R.string
                    .msg_retract_all_current_confirmations_for_this_occurrence_the_correction_rem
            TextKey.EVERY_HOW_MANY_11000 -> R.string.msg_every_how_many_11000
            TextKey.EVERYTHING_BOUGHT_YOUR_SHOPPING_LIST_IS_EMPTY ->
                R.string.msg_everything_bought_your_shopping_list_is_empty
            TextKey.EVERYTHING_THAT_MAKES_YOUR_FAMILY_DAY_YOUR_OWN ->
                R.string.msg_everything_that_makes_your_family_day_your_own
            TextKey.SHOW_DAILY_THOUGHTS -> R.string.msg_show_daily_thoughts
            TextKey.MARK_AS_COMPLETED -> R.string.msg_mark_as_completed
            TextKey.REMINDERS_FOR_YOUR_DUE_TASKS_AND_APPOINTMENTS_INCLUDING_THOSE_ASSIGNED_T ->
                R.string
                    .msg_reminders_for_your_due_tasks_and_appointments_including_those_assigned_t
            TextKey.SIGNING_IN -> R.string.msg_signing_in
            TextKey.YOUR_SIGN_IN_FOR_HAS_EXPIRED_PLEASE_SIGN_IN_AGAIN ->
                R.string.msg_your_sign_in_for_has_expired_please_sign_in_again
            TextKey.SIGN_IN_WAS_NOT_COMPLETED_CHECK_GOOGLE_CONFIGURATION_INTERNET_AND_SIGNIN ->
                R.string
                    .msg_sign_in_was_not_completed_check_google_configuration_internet_and_signin
            TextKey.NUMBER_OF_TASKS -> R.string.msg_number_of_tasks
            TextKey.ARCHIVED_LABELS_STAY_ON_EXISTING_ENTRIES_AND_IN_THE_HISTORY_SUGGESTIONS ->
                R.string.msg_archived_labels_stay_on_existing_entries_and_in_the_history_suggestions
            TextKey.YOU_CAN_MERGE_PRODUCTS_WITH_AND_IN_THEIR_NAMES_INTO_ONE_LINE_HERE_QUANTI ->
                R.string
                    .msg_you_can_merge_products_with_and_in_their_names_into_one_line_here_quanti
            TextKey.PAST_OR_CONFIRMED_ENTRIES_CAN_ALSO_BE_REMOVED_FROM_THE_OVERVIEW_THEIR_HI ->
                R.string
                    .msg_past_or_confirmed_entries_can_also_be_removed_from_the_overview_their_hi
            TextKey.TASK -> R.string.msg_task
            TextKey.TASK_ADDED -> R.string.msg_task_added
            TextKey.EDIT_TASK -> R.string.msg_edit_task
            TextKey.TASK_COMPLETED -> R.string.msg_task_completed
            TextKey.TASK_RESUMED -> R.string.msg_task_resumed
            TextKey.TASK_CHANGED -> R.string.msg_task_changed
            TextKey.TASK_PAUSED -> R.string.msg_task_paused
            TextKey.SAVE_TASK -> R.string.msg_save_task
            TextKey.TASK_OCCURRENCE -> R.string.msg_task_occurrence
            TextKey.OPEN_TASK_AND_CHECK_THE_SHARED_STATUS ->
                R.string.msg_open_task_and_check_the_shared_status
            TextKey.TASKS -> R.string.msg_tasks
            TextKey.TASKS_APPOINTMENTS -> R.string.msg_tasks_appointments
            TextKey.SELECT_OCCURRENCE -> R.string.msg_select_occurrence
            TextKey.SELECT_OCCURRENCE_LARGE_TEXT -> R.string.msg_select_occurrence_large_text
            TextKey.CONFIRM_OCCURRENCE -> R.string.msg_confirm_occurrence
            TextKey.OCCURRENCES -> R.string.msg_occurrences
            TextKey.SHOW_OCCURRENCES_FROM -> R.string.msg_show_occurrences_from
            TextKey.FARM -> R.string.msg_farm
            TextKey.EDIT -> R.string.msg_edit
            TextKey.APPOINTMENTS_THAT_HAVE_STARTED_REMAIN_UNCHANGED ->
                R.string.msg_appointments_that_have_started_remain_unchanged
            TextKey.KEEP_THE_LITTLE_TASKS_IN_SIGHT_AND_YOUR_MIND_FREE_FOR_BIG_MOMENTS_A_FIRS ->
                R.string
                    .msg_keep_the_little_tasks_in_sight_and_your_mind_free_for_big_moments_a_firs
            TextKey.KEEP_YOUR_APPOINTMENTS_IN_SIGHT_AND_YOUR_MIND_FREE_FOR_LIFE_S_LOVELY_THI ->
                R.string
                    .msg_keep_your_appointments_in_sight_and_your_mind_free_for_life_s_lovely_thi
            TextKey.KEEPING_THE_DATE_AND_INTERVAL_PRESERVES_THE_ORIGINAL_RHYTHM ->
                R.string.msg_keeping_the_date_and_interval_preserves_the_original_rhythm
            TextKey.BOTH_PARENTS -> R.string.msg_both_parents
            TextKey.BOTH_CHILDREN -> R.string.msg_both_children
            TextKey.ALREADY_CONFIRMED -> R.string.msg_already_confirmed
            TextKey.PLEASE_CHANGE_EXISTING_TASKS_THROUGH_EDIT_AND_SELECT_AN_OCCURRENCE ->
                R.string.msg_please_change_existing_tasks_through_edit_and_select_an_occurrence
            TextKey.CONFIRMED -> R.string.msg_confirmed
            TextKey.CONFIRMED_OCCURRENCES_AND_RECURRING_TASKS_THAT_HAVE_STARTED_REMAIN_UNCHA ->
                R.string
                    .msg_confirmed_occurrences_and_recurring_tasks_that_have_started_remain_uncha
            TextKey.CONFIRMED_OCCURRENCES_UPCOMING_OCCURRENCES_ARE_NOT_COUNTED_YET ->
                R.string.msg_confirmed_occurrences_upcoming_occurrences_are_not_counted_yet
            TextKey.THE_CONFIRMATION_COULD_NOT_BE_SAVED_PLEASE_TRY_AGAIN ->
                R.string.msg_the_confirmation_could_not_be_saved_please_try_again
            TextKey.CORRECT_CONFIRMATION -> R.string.msg_correct_confirmation
            TextKey.CONFIRMATION_RETRACTED -> R.string.msg_confirmation_retracted
            TextKey.CONFIRMATIONS -> R.string.msg_confirmations
            TextKey.CONFIRMATIONS_CORRECTIONS -> R.string.msg_confirmations_corrections
            TextKey.UNTIL_INCLUSIVE_OPTIONAL -> R.string.msg_until_inclusive_optional
            TextKey.UP_TO_6_LABELS_IN_TOTAL_YOU_CAN_MANAGE_SHARED_LABELS_IN_SETTINGS ->
                R.string.msg_up_to_6_labels_in_total_you_can_manage_shared_labels_in_settings
            TextKey.PLEASE_SPECIFY_1_TO_8_DISTINCT_TIMES ->
                R.string.msg_please_specify_1_to_8_distinct_times
            TextKey.PLEASE_CHECK_YOUR_ENTRIES -> R.string.msg_please_check_your_entries
            TextKey.PLEASE_CHECK_THE_DATE_RECIPIENT_ASSIGNMENT_AND_RECURRENCE_ONLY_SAVE_ADDS ->
                R.string
                    .msg_please_check_the_date_recipient_assignment_and_recurrence_only_save_adds
            TextKey.PLEASE_SELECT_WEEKDAYS -> R.string.msg_please_select_weekdays
            TextKey.PLEASE_ENTER_THE_INTERVAL_AS_A_WHOLE_NUMBER ->
                R.string.msg_please_enter_the_interval_as_a_whole_number
            TextKey.PLEASE_CHECK_THE_RHYTHM_KINDERALLTAG_USES_THE_LAST_DAY_OF_THE_MONTH_WHEN ->
                R.string
                    .msg_please_check_the_rhythm_kinderalltag_uses_the_last_day_of_the_month_when
            TextKey.PLEASE_SELECT_THE_REMINDER_AGAIN_THE_CALENDAR_FILE_USES_A_DIFFERENT_REMI ->
                R.string
                    .msg_please_select_the_reminder_again_the_calendar_file_uses_a_different_remi
            TextKey.PLEASE_ENTER_AN_INTERVAL_FROM_1_TO_1000 ->
                R.string.msg_please_enter_an_interval_from_1_to_1000
            TextKey.PLEASE_SELECT_A_CHILD -> R.string.msg_please_select_a_child
            TextKey.PLEASE_SELECT_ONE_CHILD_OR_BOTH_CHILDREN ->
                R.string.msg_please_select_one_child_or_both_children
            TextKey.PLEASE_SHARE_A_CALENDAR_FILE_USING_THE_FILE_PICKER ->
                R.string.msg_please_share_a_calendar_file_using_the_file_picker
            TextKey.PLEASE_SELECT_A_VALID_REMINDER -> R.string.msg_please_select_a_valid_reminder
            TextKey.PLEASE_SELECT_AN_UPCOMING_OCCURRENCE ->
                R.string.msg_please_select_an_upcoming_occurrence
            TextKey.PLEASE_SHARE_AN_APPOINTMENT_THAT_STARTS_AND_ENDS_ON_THE_SAME_DAY ->
                R.string.msg_please_share_an_appointment_that_starts_and_ends_on_the_same_day
            TextKey.PLEASE_SELECT_EXACTLY_ONE_TIME_FOR_THIS_OCCURRENCE ->
                R.string.msg_please_select_exactly_one_time_for_this_occurrence
            TextKey.PLEASE_IMPORT_NO_MORE_THAN_SHOPPING_ITEMS_AT_ONCE ->
                R.string.msg_please_import_no_more_than_shopping_items_at_once
            TextKey.PLEASE_IMPORT_NO_MORE_THAN_100_APPOINTMENTS_AT_ONCE ->
                R.string.msg_please_import_no_more_than_100_appointments_at_once
            TextKey.PLEASE_IMPORT_NO_MORE_THAN_10000_OCCURRENCES ->
                R.string.msg_please_import_no_more_than_10000_occurrences
            TextKey.PLEASE_SELECT_NO_MORE_THAN_6_LABELS ->
                R.string.msg_please_select_no_more_than_6_labels
            TextKey.PLEASE_SELECT_NO_MORE_THAN_6_SHARED_LABELS ->
                R.string.msg_please_select_no_more_than_6_shared_labels
            TextKey.PLEASE_ENTER_AT_LEAST_ONE_NEW_SHOPPING_ITEM ->
                R.string.msg_please_enter_at_least_one_new_shopping_item
            TextKey.PLEASE_USE_TIMES_WITH_MINUTE_PRECISION ->
                R.string.msg_please_use_times_with_minute_precision
            TextKey.PLEASE_SELECT_FEWER_CALENDAR_FILES ->
                R.string.msg_please_select_fewer_calendar_files
            TextKey.BLUE -> R.string.msg_blue
            TextKey.BROWN -> R.string.msg_brown
            TextKey.BEAR_COLOUR -> R.string.msg_bear_colour
            TextKey.COUNT_AND_UNTIL_CANNOT_APPEAR_TOGETHER ->
                R.string.msg_count_and_until_cannot_appear_together
            TextKey.DTEND_AND_DURATION_CANNOT_APPEAR_TOGETHER ->
                R.string.msg_dtend_and_duration_cannot_appear_together
            TextKey.THE_APP_ICON_COULD_NOT_BE_UPDATED_YET_PLEASE_REOPEN_THE_APP ->
                R.string.msg_the_app_icon_could_not_be_updated_yet_please_reopen_the_app
            TextKey.THE_DATE_MUST_BE_ON_OR_AFTER_THE_EFFECTIVE_START_DATE ->
                R.string.msg_the_date_must_be_on_or_after_the_effective_start_date
            TextKey.THE_END_DATE_IS_BEFORE_THE_EFFECTIVE_START_DATE ->
                R.string.msg_the_end_date_is_before_the_effective_start_date
            TextKey.THE_END_DATE_IS_BEFORE_THE_FIRST_APPOINTMENT ->
                R.string.msg_the_end_date_is_before_the_first_appointment
            TextKey.THE_SERIES_END_IS_OUTSIDE_THE_SUPPORTED_DATE_RANGE ->
                R.string.msg_the_series_end_is_outside_the_supported_date_range
            TextKey.THE_END_MUST_BE_AFTER_THE_START_ON_THE_SAME_DAY ->
                R.string.msg_the_end_must_be_after_the_start_on_the_same_day
            TextKey.THIS_YEAR_IS_NOT_SUPPORTED -> R.string.msg_this_year_is_not_supported
            TextKey.DATE -> R.string.msg_date
            TextKey.SELECT_DATE -> R.string.msg_select_date
            TextKey.YOU_CAN_CHANGE_THE_DATE_AND_TIME_IN_THE_NEXT_STEP ->
                R.string.msg_you_can_change_the_date_and_time_in_the_next_step
            TextKey.CLOSE_DATE_SEARCH -> R.string.msg_close_date_search
            TextKey.YOUR_ACCOUNT -> R.string.msg_your_account
            TextKey.YOUR_PARTNER -> R.string.msg_your_partner
            TextKey.YOUR_SIGN_IN_HAS_EXPIRED_PLEASE_SIGN_IN_AGAIN_AND_REPEAT_THE_CONFIRMATIO ->
                R.string
                    .msg_your_sign_in_has_expired_please_sign_in_again_and_repeat_the_confirmatio
            TextKey.THIS_ENTRY_HAS_CHANGED_PLEASE_CLOSE_AND_REOPEN_IT ->
                R.string.msg_this_entry_has_changed_please_close_and_reopen_it
            TextKey.THE_APPOINTMENT_MUST_BE_IN_THE_FUTURE ->
                R.string.msg_the_appointment_must_be_in_the_future
            TextKey.THE_ORIGINAL_ENTRY_IS_NO_LONGER_AVAILABLE ->
                R.string.msg_the_original_entry_is_no_longer_available
            TextKey.THE_CONFIRMATION_COULD_NOT_SYNCHRONIZE_PLEASE_CHECK_YOUR_CONNECTION_AND ->
                R.string.msg_the_confirmation_could_not_synchronize_please_check_your_connection_and
            TextKey.THE_CONFIRMATION_HAS_CHANGED_PLEASE_CLOSE_AND_REOPEN_IT ->
                R.string.msg_the_confirmation_has_changed_please_close_and_reopen_it
            TextKey.SETUP_IS_DESCRIBED_IN_DOCS_SETUP_MD_THE_CHECKLIST_OPENS_AUTOMATICALLY_ON ->
                R.string
                    .msg_setup_is_described_in_docs_setup_md_the_checklist_opens_automatically_on
            TextKey.FIREBASE_IS_NOT_CONFIGURED_YET_ADD_GOOGLE_SERVICES_JSON_TO_THE_APP_FOLDE ->
                R.string
                    .msg_firebase_is_not_configured_yet_add_google_services_json_to_the_app_folde
            TextKey.THE_CALENDAR_APP_COULD_NOT_BE_OPENED_PLEASE_TRY_SHARE_INSTEAD ->
                R.string.msg_the_calendar_app_could_not_be_opened_please_try_share_instead
            TextKey.THE_CALENDAR_FILE_CONTAINS_NO_COMPLETE_APPOINTMENT ->
                R.string.msg_the_calendar_file_contains_no_complete_appointment
            TextKey.THE_LIST_IS_TOO_LARGE_FOR_MANUAL_SORTING ->
                R.string.msg_the_list_is_too_large_for_manual_sorting
            TextKey.THE_LIST_HAS_CHANGED_PLEASE_TRY_MOVING_THE_ITEM_AGAIN ->
                R.string.msg_the_list_has_changed_please_try_moving_the_item_again
            TextKey.THE_ORDER_CHANGED_ON_THE_OTHER_DEVICE_PLEASE_TRY_MOVING_THE_ITEM_AGAIN ->
                R.string.msg_the_order_changed_on_the_other_device_please_try_moving_the_item_again
            TextKey.THE_SERIES_WILL_USE_EUROPE_BERLIN_PLEASE_CHECK_THE_TIME_AND_DAYLIGHT_SAV ->
                R.string
                    .msg_the_series_will_use_europe_berlin_please_check_the_time_and_daylight_sav
            TextKey.THIS_OCCURRENCE_S_SCHEDULED_TIME_IS_ALREADY_IN_THE_PAST_ONLY_UPCOMING_RE ->
                R.string
                    .msg_this_occurrence_s_scheduled_time_is_already_in_the_past_only_upcoming_re
            TextKey.THE_SHARED_CALENDAR_FILE_COULD_NOT_BE_OPENED ->
                R.string.msg_the_shared_calendar_file_could_not_be_opened
            TextKey.THE_LITTLE_ERRANDS_GO_HERE_LEAVE_ROOM_IN_YOUR_MIND_FOR_A_FAMILY_ADVENTUR ->
                R.string
                    .msg_the_little_errands_go_here_leave_room_in_your_mind_for_a_family_adventur
            TextKey.THE_BEST_GIFT_IDEAS_START_WITH_A_THOUGHT_ABOUT_SOMEONE_YOU_LOVE ->
                R.string.msg_the_best_gift_ideas_start_with_a_thought_about_someone_you_love
            TextKey.THIS_TASK_IS_PAUSED_RESUME_LETS_YOU_CHOOSE_THE_NEXT_OCCURRENCE ->
                R.string.msg_this_task_is_paused_resume_lets_you_choose_the_next_occurrence
            TextKey.THIS_TASK_HAS_CHANGED_PLEASE_CLOSE_AND_REOPEN_THE_OVERVIEW ->
                R.string.msg_this_task_has_changed_please_close_and_reopen_the_overview
            TextKey.THIS_OCCURRENCE_IS_ALREADY_CONFIRMED_AND_REMAINS_UNCHANGED_PLEASE_SELECT ->
                R.string
                    .msg_this_occurrence_is_already_confirmed_and_remains_unchanged_please_select
            TextKey.THIS_OCCURRENCE_HAS_CHANGED_BEEN_DELETED_OR_MOVED_PLEASE_CLOSE_AND_REOPE ->
                R.string
                    .msg_this_occurrence_has_changed_been_deleted_or_moved_please_close_and_reope
            TextKey.THESE_SETTINGS_APPLY_TO_THIS_PHONE_ANDROID_MAY_DELAY_DELIVERY ->
                R.string.msg_these_settings_apply_to_this_phone_android_may_delay_delivery
            TextKey.THESE_LABELS_APPLY_TO_ALL_NEW_SHOPPING_ITEMS_UP_TO_6 ->
                R.string.msg_these_labels_apply_to_all_new_shopping_items_up_to_6
            TextKey.THIS_MONTHLY_RULE_IS_NOT_SUPPORTED ->
                R.string.msg_this_monthly_rule_is_not_supported
            TextKey.THIS_DAY_OF_MONTH_RULE_IS_NOT_SUPPORTED ->
                R.string.msg_this_day_of_month_rule_is_not_supported
            TextKey.THIS_SERIES_HAS_ALREADY_ENDED_PLEASE_CHECK_THE_DATE_AND_RECURRENCE ->
                R.string.msg_this_series_has_already_ended_please_check_the_date_and_recurrence
            TextKey.THIS_TIME_ALREADY_EXISTS -> R.string.msg_this_time_already_exists
            TextKey.THIS_RECURRENCE_IS_NOT_SUPPORTED_YET ->
                R.string.msg_this_recurrence_is_not_supported_yet
            TextKey.THIS_RECURRENCE_RULE_IS_NOT_SUPPORTED_YET_PLEASE_SHARE_A_SINGLE_APPOINTM ->
                R.string
                    .msg_this_recurrence_rule_is_not_supported_yet_please_share_a_single_appointm
            TextKey.THIS_AND_ALL_FOLLOWING_OCCURRENCES ->
                R.string.msg_this_and_all_following_occurrences
            TextKey.THIS_AND_FOLLOWING_APPOINTMENTS -> R.string.msg_this_and_following_appointments
            TextKey.THIS_ENTRY_HAS_ALREADY_CHANGED -> R.string.msg_this_entry_has_already_changed
            TextKey.THIS_ENTRY_HAS_ALREADY_BEEN_RESTORED ->
                R.string.msg_this_entry_has_already_been_restored
            TextKey.THIS_ENTRY_WAS_DELETED_PLEASE_RESTORE_IT_FROM_TRASH_FIRST ->
                R.string.msg_this_entry_was_deleted_please_restore_it_from_trash_first
            TextKey.THIS_ENTRY_HAS_CHANGED_PLEASE_CLOSE_AND_REOPEN_IT_2192A4 ->
                R.string.msg_this_entry_has_changed_please_close_and_reopen_it_2192a4
            TextKey.THIS_APPOINTMENT_HAS_ALREADY_STARTED_EDITING_IS_NO_LONGER_POSSIBLE_DELET ->
                R.string
                    .msg_this_appointment_has_already_started_editing_is_no_longer_possible_delet
            TextKey.THIS_APPOINTMENT_IS_CANCELLED -> R.string.msg_this_appointment_is_cancelled
            TextKey.THIS_APPOINTMENT_HAS_CHANGED_OR_BEEN_CANCELLED ->
                R.string.msg_this_appointment_has_changed_or_been_cancelled
            TextKey.THIS_APPOINTMENT_HAS_CHANGED_BEEN_DELETED_OR_MOVED_PLEASE_CLOSE_AND_REOP ->
                R.string
                    .msg_this_appointment_has_changed_been_deleted_or_moved_please_close_and_reop
            TextKey.THIS_AND_ALL_FOLLOWING_APPOINTMENTS_WILL_BE_REPLACED_STARTED_APPOINTMENT ->
                R.string
                    .msg_this_and_all_following_appointments_will_be_replaced_started_appointment
            TextKey.THIS_LABEL_HAS_CHANGED_PLEASE_REOPEN_IT ->
                R.string.msg_this_label_has_changed_please_reopen_it
            TextKey.THIS_LABEL_HAS_CHANGED_PLEASE_CLOSE_AND_REOPEN_IT ->
                R.string.msg_this_label_has_changed_please_close_and_reopen_it
            TextKey.YOU_VE_REACHED_THE_END -> R.string.msg_you_ve_reached_the_end
            TextKey.A_SHOPPING_ITEM_IS_LONGER_THAN_120_CHARACTERS_PLEASE_SHORTEN_IT_OR_SPLIT ->
                R.string
                    .msg_a_shopping_item_is_longer_than_120_characters_please_shorten_it_or_split
            TextKey.ONE_SHOPPING_ITEM_PER_LINE -> R.string.msg_one_shopping_item_per_line
            TextKey.A_CALENDAR_IMPORT_IS_WAITING_PLEASE_SIGN_IN_AND_ENABLE_FAMILY_ACCESS_FIR ->
                R.string
                    .msg_a_calendar_import_is_waiting_please_sign_in_and_enable_family_access_fir
            TextKey.ONE_RING_SEGMENT_PER_TIME_SLOT_TAP_A_DAY_FOR_DETAILS_EMPTY_DAYS_WERE_NOT ->
                R.string
                    .msg_one_ring_segment_per_time_slot_tap_a_day_for_details_empty_days_were_not
            TextKey.A_LITTLE_EVERYDAY_MAGIC_FOR_YOUR_FAMILY ->
                R.string.msg_a_little_everyday_magic_for_your_family
            TextKey.A_BEAR_AND_A_FAVOURITE_COLOUR_FOR_EACH_CHILD ->
                R.string.msg_a_bear_and_a_favourite_colour_for_each_child
            TextKey.A_SHARED_VIEW_OF_YOUR_FAMILY_S_SMALL_AND_BIG_TASKS ->
                R.string.msg_a_shared_view_of_your_family_s_small_and_big_tasks
            TextKey.A_SHARED_VIEW_OF_YOUR_DAY -> R.string.msg_a_shared_view_of_your_day
            TextKey.A_WELL_FILLED_SHOPPING_BASKET_AND_TIME_FOR_THE_PEOPLE_YOU_LOVE ->
                R.string.msg_a_well_filled_shopping_basket_and_time_for_the_people_you_love
            TextKey.A_CLEAR_OVERVIEW_MAKES_EVERYDAY_LIFE_EASIER_SO_YOU_CAN_BE_FULLY_THERE_FO ->
                R.string
                    .msg_a_clear_overview_makes_everyday_life_easier_so_you_can_be_fully_there_fo
            TextKey.A_LITTLE_MOMENT_TO_TAKE_A_BREATH ->
                R.string.msg_a_little_moment_to_take_a_breath
            TextKey.A_LITTLE_GIFT_A_BIG_SMILE_LET_YOUR_IDEAS_GROW_HERE ->
                R.string.msg_a_little_gift_a_big_smile_let_your_ideas_grow_here
            TextKey.A_SIMILAR_APPOINTMENT_ALREADY_EXISTS ->
                R.string.msg_a_similar_appointment_already_exists
            TextKey.AN_EARLY_CONFIRMATION_COUNTS_FOR_THIS_OCCURRENCE_THE_HISTORY_RECORDS_THE ->
                R.string
                    .msg_an_early_confirmation_counts_for_this_occurrence_the_history_records_the
            TextKey.A_LATE_CONFIRMATION_RECORDS_THE_CURRENT_TIME ->
                R.string.msg_a_late_confirmation_records_the_current_time
            TextKey.SOME_SHOPPING_ITEMS_COULD_NOT_SYNCHRONIZE_PLEASE_CHECK_THE_LIST_AND_FAMI ->
                R.string
                    .msg_some_shopping_items_could_not_synchronize_please_check_the_list_and_fami
            TextKey.SHOPPING_ITEM_DELETED -> R.string.msg_shopping_item_deleted
            TextKey.SHOPPING_ITEM_CHANGED -> R.string.msg_shopping_item_changed
            TextKey.ADD_SHOPPING_ITEM -> R.string.msg_add_shopping_item
            TextKey.SHOPPING_ITEM_REOPENED -> R.string.msg_shopping_item_reopened
            TextKey.SHOPPING_LIST -> R.string.msg_shopping_list
            TextKey.SHOPPING -> R.string.msg_shopping
            TextKey.SHOPPING_GIFT_IDEAS -> R.string.msg_shopping_gift_ideas
            TextKey.ADD_SHOPPING_ITEMS -> R.string.msg_add_shopping_items
            TextKey.SHOPPING_ITEMS_COULD_NOT_BE_ADDED ->
                R.string.msg_shopping_items_could_not_be_added
            TextKey.ONE_OFF -> R.string.msg_one_off
            TextKey.ONE_OFF_TASKS_AND_FIXED_WEEKDAYS_DO_NOT_HAVE_AN_ADDITIONAL_INTERVAL ->
                R.string.msg_one_off_tasks_and_fixed_weekdays_do_not_have_an_additional_interval
            TextKey.ONE_OFF_APPOINTMENTS_HAVE_NO_INTERVAL ->
                R.string.msg_one_off_appointments_have_no_interval
            TextKey.SETTINGS -> R.string.msg_settings
            TextKey.THE_ENTRY_COULD_NOT_SYNCHRONIZE_PLEASE_CHECK_FAMILY_ACCESS_AND_RULES ->
                R.string.msg_the_entry_could_not_synchronize_please_check_family_access_and_rules
            TextKey.DELETE_ENTRY -> R.string.msg_delete_entry
            TextKey.PARENT -> R.string.msg_parent
            TextKey.PARENT_UNAVAILABLE_PLEASE_RELOAD_FAMILY_ACCESS ->
                R.string.msg_parent_unavailable_please_reload_family_access
            TextKey.REMINDER -> R.string.msg_reminder
            TextKey.REMINDERS -> R.string.msg_reminders
            TextKey.COMPLETED_BY -> R.string.msg_completed_by
            TextKey.COMPLETED_TASKS -> R.string.msg_completed_tasks
            TextKey.COMPLETED_TASKS_EACH_OCCURRENCE_COUNTS_ONCE_USING_ITS_FIRST_STILL_VALID ->
                R.string.msg_completed_tasks_each_occurrence_counts_once_using_its_first_still_valid
            TextKey.THERE_IS_NO_VISIBLE_OCCURRENCE -> R.string.msg_there_is_no_visible_occurrence
            TextKey.YOUR_FARM -> R.string.msg_your_farm
            TextKey.YOUR_SHOPPING_LIST -> R.string.msg_your_shopping_list
            TextKey.YOUR_VEHICLE_WORLD -> R.string.msg_your_vehicle_world
            TextKey.YOUR_GIFT_IDEAS -> R.string.msg_your_gift_ideas
            TextKey.YOUR_CHILDREN -> R.string.msg_your_children
            TextKey.YOUR_NATURE_WORLD -> R.string.msg_your_nature_world
            TextKey.YOUR_SPACE_STATION -> R.string.msg_your_space_station
            TextKey.YOUR_PLAY_WORLD -> R.string.msg_your_play_world
            TextKey.YOUR_APPOINTMENTS -> R.string.msg_your_appointments
            TextKey.YOUR_ANIMAL_WORLD -> R.string.msg_your_animal_world
            TextKey.YOUR_MAGICAL_WORLD -> R.string.msg_your_magical_world
            TextKey.VEHICLE_WORLD -> R.string.msg_vehicle_world
            TextKey.COLOURS_LITTLE_COMPANIONS_AND_APP_ICON_ONLY_ON_THIS_DEVICE ->
                R.string.msg_colours_little_companions_and_app_icon_only_on_this_device
            TextKey.FIRESTORE_DENIED_THIS_CONFIRMATION_PLEASE_CHECK_THE_FIREBASE_PROJECT_FAM ->
                R.string
                    .msg_firestore_denied_this_confirmation_please_check_the_firebase_project_fam
            TextKey.RESUME -> R.string.msg_resume
            TextKey.FOR -> R.string.msg_recipient_for
            TextKey.FOR_FERDINAND_AND_JOHANN_AND_FOR_BOTH_OF_YOU ->
                R.string.msg_for_ferdinand_and_johann_and_for_both_of_you
            TextKey.THIS_CHANGE_REQUIRES_A_CURRENT_SERVER_CONNECTION ->
                R.string.msg_this_change_requires_a_current_server_connection
            TextKey.NO_OCCURRENCE_IS_SCHEDULED_FOR_THIS_DAY ->
                R.string.msg_no_occurrence_is_scheduled_for_this_day
            TextKey.THERE_ARE_NO_TASKS_FOR_THIS_DAY -> R.string.msg_there_are_no_tasks_for_this_day
            TextKey.THERE_ARE_NO_APPOINTMENTS_FOR_THIS_DAY ->
                R.string.msg_there_are_no_appointments_for_this_day
            TextKey.THERE_ARE_NO_TASKS_FOR_THIS_DAY_AND_THE_SELECTED_PERIOD ->
                R.string.msg_there_are_no_tasks_for_this_day_and_the_selected_period
            TextKey.WHICH_AREA -> R.string.msg_which_area
            TextKey.WHO_IS_IT_FOR -> R.string.msg_who_is_it_for
            TextKey.ALL_DAY -> R.string.msg_all_day
            TextKey.GARDEN -> R.string.msg_garden
            TextKey.BOUGHT -> R.string.msg_bought
            TextKey.BOUGHT_5F96A7 -> R.string.msg_bought_5f96a7
            TextKey.BOUGHT_SHOPPING_ITEMS_GIFT_IDEAS ->
                R.string.msg_bought_shopping_items_gift_ideas
            TextKey.PURCHASED_ENTRIES_COUNT_ONCE_USING_THEIR_FIRST_STILL_VALID_PURCHASE_CONF ->
                R.string
                    .msg_purchased_entries_count_once_using_their_first_still_valid_purchase_conf
            TextKey.DELETED_TASKS -> R.string.msg_deleted_tasks
            TextKey.DELETED_OCCURRENCES_AND_SERIES_CONFIRMATIONS_HISTORY_AND_STATISTICS_ARE ->
                R.string.msg_deleted_occurrences_and_series_confirmations_history_and_statistics_are
            TextKey.DELETED_APPOINTMENTS -> R.string.msg_deleted_appointments
            TextKey.SHARED_LABELS_FOR_YOUR_SHOPPING_GIFT_OCCASIONS_AND_GIFT_GIVERS ->
                R.string.msg_shared_labels_for_your_shopping_gift_occasions_and_gift_givers
            TextKey.GIFT_OCCASIONS -> R.string.msg_gift_occasions
            TextKey.GIFT_IDEA_DELETED -> R.string.msg_gift_idea_deleted
            TextKey.GIFT_IDEA_CHANGED -> R.string.msg_gift_idea_changed
            TextKey.GIFT_IDEA_REOPENED -> R.string.msg_gift_idea_reopened
            TextKey.GIFT_IDEAS -> R.string.msg_gift_ideas
            TextKey.NOTHING_IS_SAVED_UNTIL_YOU_TAP_ADD_OFFLINE_SHOPPING_ITEMS_ARE_QUEUED_IND ->
                R.string
                    .msg_nothing_is_saved_until_you_tap_add_offline_shopping_items_are_queued_ind
            TextKey.STORED_DATA_FOR_IS_INVALID_PLEASE_CHECK_THE_DATA_FORMAT ->
                R.string.msg_stored_data_for_is_invalid_please_check_the_data_format
            TextKey.SAVED_DATA_CONNECTING -> R.string.msg_saved_data_connecting
            TextKey.SAVED_DATA_SOME_CONFIRMATIONS_MAY_BE_MISSING ->
                R.string.msg_saved_data_some_confirmations_may_be_missing
            TextKey.SAVED_DATA_BUYING_AND_REOPENING_ARE_QUEUED_OFFLINE_EDITING_AND_DELETING ->
                R.string.msg_saved_data_buying_and_reopening_are_queued_offline_editing_and_deleting
            TextKey.SAVED_DATA_EDITING_OR_DELETING_REQUIRES_A_CONNECTION ->
                R.string.msg_saved_data_editing_or_deleting_requires_a_connection
            TextKey.SAVED_DATA_CONFIRMATIONS_FROM_THE_OTHER_PHONE_MAY_NOT_BE_VISIBLE_YET ->
                R.string.msg_saved_data_confirmations_from_the_other_phone_may_not_be_visible_yet
            TextKey.COUNTS_USE_THE_DAY_THE_EVENT_WAS_RECORDED_NOT_ITS_SCHEDULED_DAY_CONFIRMA ->
                R.string
                    .msg_counts_use_the_day_the_event_was_recorded_not_its_scheduled_day_confirma
            TextKey.GOOGLE_SIGN_IN_IS_NOT_CONFIGURED_YET_ENABLE_GOOGLE_IN_FIREBASE_AUTHENTIC ->
                R.string
                    .msg_google_sign_in_is_not_configured_yet_enable_google_in_firebase_authentic
            TextKey.GREEN -> R.string.msg_green
            TextKey.HELLO_YOUR_FAMILY_ACCESS_NEEDS_TO_BE_ENABLED_THIS_USER_ID_IS_REQUIRED ->
                R.string.msg_hello_your_family_access_needs_to_be_enabled_this_user_id_is_required
            TextKey.KEEP_YOUR_PLANS_HERE_AND_COLLECT_MEMORIES_ALONG_THE_WAY_NEW_FRIENDS_BRAV ->
                R.string
                    .msg_keep_your_plans_here_and_collect_memories_along_the_way_new_friends_brav
            TextKey.HOUSEHOLD -> R.string.msg_household
            TextKey.TODAY -> R.string.msg_today
            TextKey.YOUR_TASKS_HAVE_A_HOME_HERE_LEAVE_MORE_ROOM_FOR_PICTURE_BOOK_ADVENTURES ->
                R.string.msg_your_tasks_have_a_home_here_leave_more_room_for_picture_book_adventures
            TextKey.YOUR_APPOINTMENTS_HAVE_A_HOME_HERE_LEAVE_MORE_ROOM_FOR_SPONTANEOUS_PLAYG ->
                R.string
                    .msg_your_appointments_have_a_home_here_leave_more_room_for_spontaneous_playg
            TextKey.HERE_S_ROOM_FOR_YOUR_NEXT_GIFT_IDEAS ->
                R.string.msg_here_s_room_for_your_next_gift_ideas
            TextKey.EVERYTHING_IS_BOUGHT_ADD_NEW_ITEMS_WITH_SHOPPING ->
                R.string.msg_everything_is_bought_add_new_items_with_shopping
            TextKey.THERE_IS_NOTHING_IN_TRASH_YET -> R.string.msg_there_is_nothing_in_trash_yet
            TextKey.DISMISS_NOTICE -> R.string.msg_dismiss_notice
            TextKey.HONEY -> R.string.msg_honey
            TextKey.UP_TO_1000_ENTRIES_CAN_BE_MANUALLY_SORTED ->
                R.string.msg_up_to_1000_entries_can_be_manually_sorted
            TextKey.I_HAVE_CHECKED_THE_IMPORT_NOTES -> R.string.msg_i_have_checked_the_import_notes
            TextKey.SAVE_TO_CALENDAR_OPENS_YOUR_CALENDAR_APP_FOR_CONFIRMATION_ONLY_THIS_APPO ->
                R.string
                    .msg_save_to_calendar_opens_your_calendar_app_for_confirmation_only_this_appo
            TextKey.THE_INTERVAL_MUST_BE_BETWEEN_1_AND_1000 ->
                R.string.msg_the_interval_must_be_between_1_and_1000
            TextKey.YES_RETRACT_CONFIRMATIONS -> R.string.msg_yes_retract_confirmations
            TextKey.YES_DELETE -> R.string.msg_yes_delete
            TextKey.YEARS -> R.string.msg_years
            TextKey.ONLY_THE_NEXT_OPEN_OCCURRENCE_OF_EACH_TASK_IS_SHOWN ->
                R.string.msg_only_the_next_open_occurrence_of_each_task_is_shown
            TextKey.SYNCHRONIZE_NOW -> R.string.msg_synchronize_now
            TextKey.SHOW_CALENDAR_VIEW -> R.string.msg_show_calendar_view
            TextKey.THE_CALENDAR_FILE_IS_LARGER_THAN_1_MB ->
                R.string.msg_the_calendar_file_is_larger_than_1_mb
            TextKey.THE_CALENDAR_FILE_IS_TOO_LARGE -> R.string.msg_the_calendar_file_is_too_large
            TextKey.THE_CALENDAR_FILE_COULD_NOT_BE_READ ->
                R.string.msg_the_calendar_file_could_not_be_read
            TextKey.THE_CALENDAR_FILE_COULD_NOT_BE_OPENED ->
                R.string.msg_the_calendar_file_could_not_be_opened
            TextKey.CALENDAR_FILES_MAY_NOT_EXCEED_1_MB_IN_TOTAL ->
                R.string.msg_calendar_files_may_not_exceed_1_mb_in_total
            TextKey.CALENDAR_FORMAT_NOT_SUPPORTED -> R.string.msg_calendar_format_not_supported
            TextKey.THE_CALENDAR_IMPORT_COULD_NOT_BE_LOADED ->
                R.string.msg_the_calendar_import_could_not_be_loaded
            TextKey.THE_CALENDAR_IMPORT_COULD_NOT_BE_CACHED ->
                R.string.msg_the_calendar_import_could_not_be_cached
            TextKey.THE_PURCHASE_STATUS_COULD_NOT_BE_CHANGED ->
                R.string.msg_the_purchase_status_could_not_be_changed
            TextKey.THE_PURCHASE_STATUS_COULD_NOT_SYNCHRONIZE_PLEASE_TRY_AGAIN ->
                R.string.msg_the_purchase_status_could_not_synchronize_please_try_again
            TextKey.NO_MATCHING_APP_AVAILABLE_YOU_CAN_COPY_THE_DETAILS_ABOVE ->
                R.string.msg_no_matching_app_available_you_can_copy_the_details_above
            TextKey.NONE -> R.string.msg_none
            TextKey.NO_TASKS_IN_THE_SELECTED_PERIOD -> R.string.msg_no_tasks_in_the_selected_period
            TextKey.NO_OCCURRENCE_SCHEDULED -> R.string.msg_no_occurrence_scheduled
            TextKey.NO_END_TIME_SPECIFIED_PLEASE_CHECK_THE_SUGGESTED_ONE_HOUR_DURATION ->
                R.string.msg_no_end_time_specified_please_check_the_suggested_one_hour_duration
            TextKey.NO_EVENTS -> R.string.msg_no_events
            TextKey.NO_EVENTS_IN_THIS_PERIOD -> R.string.msg_no_events_in_this_period
            TextKey.NO_CALENDAR_FILE_RECEIVED -> R.string.msg_no_calendar_file_received
            TextKey.NO_SERVER_CONNECTION_FOR_SAVED_DATA_REMAINS_AVAILABLE_PLEASE_TRY_AGAIN ->
                R.string.msg_no_server_connection_for_saved_data_remains_available_please_try_again
            TextKey.SPEECH_RECOGNITION_UNAVAILABLE_PLEASE_USE_THE_KEYBOARD ->
                R.string.msg_speech_recognition_unavailable_please_use_the_keyboard
            TextKey.NO_APPOINTMENTS_IN_THIS_PERIOD -> R.string.msg_no_appointments_in_this_period
            TextKey.NO_UPCOMING_TASKS_ADD_NEW_TASKS_WITH_TASK ->
                R.string.msg_no_upcoming_tasks_add_new_tasks_with_task
            TextKey.NOT_A_VALID_ICALENDAR_FILE_ICS -> R.string.msg_not_a_valid_icalendar_file_ics
            TextKey.NO_UPCOMING_UNCONFIRMED_OCCURRENCE_FOUND ->
                R.string.msg_no_upcoming_unconfirmed_occurrence_found
            TextKey.NO_MATCHING_CALENDAR_APP_FOUND_YOU_CAN_USE_SHARE_INSTEAD ->
                R.string.msg_no_matching_calendar_app_found_you_can_use_share_instead
            TextKey.NO_SUPPORTED_APPOINTMENTS_FOUND -> R.string.msg_no_supported_appointments_found
            TextKey.THE_CHILD_PROFILE_COULD_NOT_BE_SAVED_CHECK_YOUR_CONNECTION_AND_ACCESS_RU ->
                R.string
                    .msg_the_child_profile_could_not_be_saved_check_your_connection_and_access_ru
            TextKey.LITTLE_TASKS_FOR_YOUR_FAMILY_GARDEN ->
                R.string.msg_little_tasks_for_your_family_garden
            TextKey.LITTLE_TASKS_IN_SIGHT_TOGETHER -> R.string.msg_little_tasks_in_sight_together
            TextKey.LITTLE_STEPS_DAY_BY_DAY -> R.string.msg_little_steps_day_by_day
            TextKey.SHOW_UPCOMING_TASKS -> R.string.msg_show_upcoming_tasks
            TextKey.PAUSE_UPCOMING_OCCURRENCES -> R.string.msg_pause_upcoming_occurrences
            TextKey.SHOW_UPCOMING_APPOINTMENTS -> R.string.msg_show_upcoming_appointments
            TextKey.CORRECTIONS -> R.string.msg_corrections
            TextKey.LABEL_UNAVAILABLE_PLEASE_RELOAD -> R.string.msg_label_unavailable_please_reload
            TextKey.YOU_CAN_CREATE_LABELS_IN_SETTINGS ->
                R.string.msg_you_can_create_labels_in_settings
            TextKey.THE_LAUNCHER_ICON_COULD_NOT_BE_UPDATED ->
                R.string.msg_the_launcher_icon_could_not_be_updated
            TextKey.PURPLE -> R.string.msg_purple
            TextKey.DELETE -> R.string.msg_delete
            TextKey.DELETE_F4B324 -> R.string.msg_delete_f4b324
            TextKey.SOME_THINGS_NEED_DOING_KEEP_THEM_HERE_AND_ENJOY_THE_MOMENTS_IN_BETWEEN_M ->
                R.string
                    .msg_some_things_need_doing_keep_them_here_and_enjoy_the_moments_in_between_m
            TextKey.MORE_UPCOMING_TASKS -> R.string.msg_more_upcoming_tasks
            TextKey.PLEASE_SHARE_MULTIPLE_OR_SPECIAL_WEEKDAYS_AS_INDIVIDUAL_APPOINTMENTS ->
                R.string.msg_please_share_multiple_or_special_weekdays_as_individual_appointments
            TextKey.MULTI_DAY_APPOINTMENTS_ARE_NOT_SUPPORTED_YET ->
                R.string.msg_multi_day_appointments_are_not_supported_yet
            TextKey.MILK_BREAD_AND_A_LITTLE_ANTICIPATION_KEEP_YOUR_MINDS_FREE_FOR_FAMILY_TIM ->
                R.string
                    .msg_milk_bread_and_a_little_anticipation_keep_your_minds_free_for_family_tim
            TextKey.MONTHS -> R.string.msg_months
            TextKey.MONTHS_1_TO_1000 -> R.string.msg_months_1_to_1000
            TextKey.MONTHLY -> R.string.msg_monthly
            TextKey.BY_PARENT -> R.string.msg_by_parent
            TextKey.NAME_BEAR_COLOUR -> R.string.msg_name_bear_colour
            TextKey.NATURE_WORLD -> R.string.msg_nature_world
            TextKey.NEW_TASK -> R.string.msg_new_task
            TextKey.NEW_TASKS -> R.string.msg_new_tasks
            TextKey.NEW_TASKS_START_NO_EARLIER_THAN_TODAY ->
                R.string.msg_new_tasks_start_no_earlier_than_today
            TextKey.NEW_APPOINTMENTS -> R.string.msg_new_appointments
            TextKey.NEW_APPOINTMENTS_MUST_BE_IN_THE_FUTURE ->
                R.string.msg_new_appointments_must_be_in_the_future
            TextKey.NEW_APPOINTMENT -> R.string.msg_new_appointment
            TextKey.UPDATES_ARRIVE_WHILE_THE_APP_IS_RUNNING_AND_CONNECTED_PUSH_NOTIFICATIONS ->
                R.string
                    .msg_updates_arrive_while_the_app_is_running_and_connected_push_notifications
            TextKey.NOT_CONFIRMED -> R.string.msg_not_confirmed
            TextKey.NO_PARENTS_AVAILABLE_YET -> R.string.msg_no_parents_available_yet
            TextKey.NO_LABELS_YET_CREATE_YOUR_OWN_OR_USE_THE_SUGGESTIONS ->
                R.string.msg_no_labels_yet_create_your_own_or_use_the_suggestions
            TextKey.NO_DUE_OR_CONFIRMED_OCCURRENCES_YET ->
                R.string.msg_no_due_or_confirmed_occurrences_yet
            TextKey.NOT_CONFIRMED_YET -> R.string.msg_not_confirmed_yet
            TextKey.NOTES -> R.string.msg_notes
            TextKey.NOTES_OPTIONAL -> R.string.msg_notes_optional
            TextKey.NOTES_UP_TO_2000_CHARACTERS -> R.string.msg_notes_up_to_2000_characters
            TextKey.ONLY_THIS_OCCURRENCE -> R.string.msg_only_this_occurrence
            TextKey.ONLY_THIS_APPOINTMENT -> R.string.msg_only_this_appointment
            TextKey.ONLY_MINE -> R.string.msg_only_mine
            TextKey.NEXT_90_TASKS -> R.string.msg_next_90_tasks
            TextKey.NEXT_90_DAYS -> R.string.msg_next_90_days
            TextKey.NEXT_OCCURRENCE -> R.string.msg_next_occurrence
            TextKey.NEXT_WEEK -> R.string.msg_next_week
            TextKey.NEXT_MONTH -> R.string.msg_next_month
            TextKey.NEXT_DAY -> R.string.msg_next_day
            TextKey.OPEN -> R.string.msg_open
            TextKey.OPEN_ONE_OFF_TASKS_STAY_VISIBLE_FOR_THE_SELECTED_PERIOD_FOR_RECURRING_TA ->
                R.string
                    .msg_open_one_off_tasks_stay_visible_for_the_selected_period_for_recurring_ta
            TextKey.LOCATION -> R.string.msg_location
            TextKey.LOCATION_OPTIONAL -> R.string.msg_location_optional
            TextKey.LOCATION_OR_NOTES_ARE_TOO_LONG -> R.string.msg_location_or_notes_are_too_long
            TextKey.TRASH -> R.string.msg_trash
            TextKey.PAUSE -> R.string.msg_pause
            TextKey.SHOW_PAUSED_TASKS -> R.string.msg_show_paused_tasks
            TextKey.HIDE_PAUSED_TASKS -> R.string.msg_hide_paused_tasks
            TextKey.ROOM_FOR_A_LITTLE_EVERYDAY_HELPER ->
                R.string.msg_room_for_a_little_everyday_helper
            TextKey.REVIEW -> R.string.msg_review
            TextKey.THE_ORDER_COULD_NOT_BE_SAVED_PLEASE_CHECK_YOUR_CONNECTION_AND_RULES ->
                R.string.msg_the_order_could_not_be_saved_please_check_your_connection_and_rules
            TextKey.SAVING_ORDER -> R.string.msg_saving_order
            TextKey.PINK -> R.string.msg_pink
            TextKey.CLOSE -> R.string.msg_close
            TextKey.PLEASE_SHARE_SERIES_WITH_INDIVIDUAL_EXCEPTIONS_AS_SEPARATE_APPOINTMENTS ->
                R.string.msg_please_share_series_with_individual_exceptions_as_separate_appointments
            TextKey.SERVER_LIMIT_REACHED_FOR_PLEASE_CHECK_FIREBASE_USAGE ->
                R.string.msg_server_limit_reached_for_please_check_firebase_usage
            TextKey.SAVE -> R.string.msg_save
            TextKey.SAVING_FAILED_PLEASE_CHECK_YOUR_CONNECTION_AND_FAMILY_ACCESS ->
                R.string.msg_saving_failed_please_check_your_connection_and_family_access
            TextKey.SAVING -> R.string.msg_saving
            TextKey.PLAY_WORLD -> R.string.msg_play_world
            TextKey.SPEECH_RECOGNITION_COULD_NOT_BE_OPENED_PLEASE_USE_THE_KEYBOARD ->
                R.string.msg_speech_recognition_could_not_be_opened_please_use_the_keyboard
            TextKey.STATISTICS -> R.string.msg_statistics
            TextKey.STATISTICS_LARGE_TEXT -> R.string.msg_statistics_large_text
            TextKey.SYNCHRONIZATION_OF_FAILED_PLEASE_TRY_AGAIN ->
                R.string.msg_synchronization_of_failed_please_try_again
            TextKey.DAYS_E94F59 -> R.string.msg_days_e94f59
            TextKey.DAYS_1_TO_1000 -> R.string.msg_days_1_to_1000
            TextKey.DAY_BOUNDARIES_AND_REMINDERS_FOLLOW_YOUR_FAMILY_TIMEZONE_EVEN_WHEN_TRAVE ->
                R.string
                    .msg_day_boundaries_and_reminders_follow_your_family_timezone_even_when_trave
            TextKey.DAY_OVERVIEW_EVENTS -> R.string.msg_day_overview_events
            TextKey.SHARE -> R.string.msg_share
            TextKey.SHARE_THE_TASKS_AND_COLLECT_MEMORIES_TOGETHER_LITTLE_DISCOVERIES_BRAVE_S ->
                R.string
                    .msg_share_the_tasks_and_collect_memories_together_little_discoveries_brave_s
            TextKey.APPOINTMENT -> R.string.msg_appointment
            TextKey.APPOINTMENT_8435F7 -> R.string.msg_appointment_8435f7
            TextKey.APPOINTMENT_CANCELLED -> R.string.msg_appointment_cancelled
            TextKey.APPOINTMENT_ADDED -> R.string.msg_appointment_added
            TextKey.EDIT_APPOINTMENT -> R.string.msg_edit_appointment
            TextKey.THE_APPOINTMENT_ALREADY_EXISTS_PLEASE_RELOAD ->
                R.string.msg_the_appointment_already_exists_please_reload
            TextKey.APPOINTMENT_CHANGED -> R.string.msg_appointment_changed
            TextKey.THE_APPOINTMENT_COULD_NOT_BE_SAVED_CHECK_YOUR_CONNECTION_AND_ACCESS_RULE ->
                R.string
                    .msg_the_appointment_could_not_be_saved_check_your_connection_and_access_rule
            TextKey.THE_APPOINTMENT_COULD_NOT_BE_SHARED ->
                R.string.msg_the_appointment_could_not_be_shared
            TextKey.SAVE_APPOINTMENT -> R.string.msg_save_appointment
            TextKey.SHARE_APPOINTMENT -> R.string.msg_share_appointment
            TextKey.THIS_AND_FOLLOWING_APPOINTMENTS_CANCELLED ->
                R.string.msg_this_and_following_appointments_cancelled
            TextKey.THIS_AND_FOLLOWING_APPOINTMENTS_CHANGED ->
                R.string.msg_this_and_following_appointments_changed
            TextKey.OPEN_APPOINTMENT_AND_CHECK_DETAILS ->
                R.string.msg_open_appointment_and_check_details
            TextKey.IMPORT_APPOINTMENT -> R.string.msg_import_appointment
            TextKey.APPOINTMENTS -> R.string.msg_appointments
            TextKey.APPOINTMENT_CHANGES -> R.string.msg_appointment_changes
            TextKey.ANIMAL_WORLD -> R.string.msg_animal_world
            TextKey.ENTER_ONE_ITEM_PER_LINE_OR_DICTATE_YOUR_LIST_FOR_EXAMPLE_MILK_AND_BREAD ->
                R.string.msg_enter_one_item_per_line_or_dictate_your_list_for_example_milk_and_bread
            TextKey.DAILY -> R.string.msg_daily
            TextKey.TIME -> R.string.msg_time
            TextKey.TIME_A3636D -> R.string.msg_time_a3636d
            TextKey.ADD_TIME_8 -> R.string.msg_add_time_8
            TextKey.INVALID_ORDER_PLEASE_REOPEN_THE_LIST ->
                R.string.msg_invalid_order_please_reopen_the_list
            TextKey.INVALID_ORDER_VERSION -> R.string.msg_invalid_order_version
            TextKey.INVALID_NESTED_APPOINTMENTS -> R.string.msg_invalid_nested_appointments
            TextKey.INVALID_WEEK_START -> R.string.msg_invalid_week_start
            TextKey.SHOW_PAST_TASKS -> R.string.msg_show_past_tasks
            TextKey.PAST_TASKS_IN_DAYS -> R.string.msg_past_tasks_in_days
            TextKey.PAST_OCCURRENCE -> R.string.msg_past_occurrence
            TextKey.PAST_OCCURRENCES_REMAIN_UNCHANGED_YOU_CAN_DELETE_THEM_AND_RESTORE_THEM_F ->
                R.string
                    .msg_past_occurrences_remain_unchanged_you_can_delete_them_and_restore_them_f
            TextKey.HISTORY -> R.string.msg_history
            TextKey.MANAGE_THE_TASKS_THAT_NEED_DOING_AND_MAKE_ROOM_IN_YOUR_MIND_FOR_LIFE_S_L ->
                R.string
                    .msg_manage_the_tasks_that_need_doing_and_make_room_in_your_mind_for_life_s_l
            TextKey.FROM_THE_PAEDIATRICIAN_TO_A_FAMILY_TRIP_PLAN_WHAT_MATTERS_TO_YOU_AND_ENJ ->
                R.string
                    .msg_from_the_paediatrician_to_a_family_trip_plan_what_matters_to_you_and_enj
            TextKey.PLEASE_SYNCHRONIZE_PENDING_CHANGES_BEFORE_SIGNING_OUT ->
                R.string.msg_please_synchronize_pending_changes_before_signing_out
            TextKey.PREVIOUS_90_TASKS -> R.string.msg_previous_90_tasks
            TextKey.PREVIOUS_90_DAYS -> R.string.msg_previous_90_days
            TextKey.SUGGESTIONS -> R.string.msg_suggestions
            TextKey.OPEN_WEBSITE -> R.string.msg_open_website
            TextKey.CONTINUE -> R.string.msg_continue
            TextKey.DICTATE_MORE_SHOPPING_ITEMS -> R.string.msg_dictate_more_shopping_items
            TextKey.OPEN_MORE_PAGES -> R.string.msg_open_more_pages
            TextKey.WHICH_OCCURRENCE -> R.string.msg_which_occurrence
            TextKey.SPACE -> R.string.msg_space
            TextKey.FEWER_UPCOMING_TASKS -> R.string.msg_fewer_upcoming_tasks
            TextKey.WHEN_THE_OTHER_PARENT_ADDS_A_TASK ->
                R.string.msg_when_the_other_parent_adds_a_task
            TextKey.WHEN_THE_OTHER_PARENT_CONFIRMS_AN_OPEN_TASK_WHILE_KINDERALLTAG_IS_RUNNIN ->
                R.string
                    .msg_when_the_other_parent_confirms_an_open_task_while_kinderalltag_is_runnin
            TextKey.WHEN_THE_OTHER_PARENT_ADDS_AN_APPOINTMENT ->
                R.string.msg_when_the_other_parent_adds_an_appointment
            TextKey.REOPEN -> R.string.msg_reopen
            TextKey.RESTORED_ANOTHER_DELETION_STILL_HIDES_SOME_PARTS ->
                R.string.msg_restored_another_deletion_still_hides_some_parts
            TextKey.RESTORED_TASKS -> R.string.msg_restored_tasks
            TextKey.RESTORED_APPOINTMENTS -> R.string.msg_restored_appointments
            TextKey.RECURRENCE -> R.string.msg_recurrence
            TextKey.WEEKS -> R.string.msg_weeks
            TextKey.WEEKS_1_TO_1000 -> R.string.msg_weeks_1_to_1000
            TextKey.SELECT_THE_DATE_AND_TIME_OF_THE_ENTRY_YOU_WANT_TO_REMOVE ->
                R.string.msg_select_the_date_and_time_of_the_entry_you_want_to_remove
            TextKey.CHOOSE_A_DAY_AND_TIME_TO_CONFIRM_AN_OCCURRENCE_INCLUDING_IN_ADVANCE ->
                R.string.msg_choose_a_day_and_time_to_confirm_an_occurrence_including_in_advance
            TextKey.SELECT_THE_SCOPE_BEFORE_TAPPING_EDIT ->
                R.string.msg_select_the_scope_before_tapping_edit
            TextKey.SELECT_THE_OCCURRENCE_YOU_WANT_TO_CHANGE_YOU_CAN_SET_THE_NEW_DATE_AFTERW ->
                R.string
                    .msg_select_the_occurrence_you_want_to_change_you_can_set_the_new_date_afterw
            TextKey.WEEKLY -> R.string.msg_weekly
            TextKey.SHOWS_THE_CALENDAR_IN_TASKS_AND_APPOINTMENTS ->
                R.string.msg_shows_the_calendar_in_tasks_and_appointments
            TextKey.SHOWS_ROTATING_MESSAGES_IN_TASKS_APPOINTMENTS_SHOPPING_LIST_AND_GIFT_IDE ->
                R.string
                    .msg_shows_rotating_messages_in_tasks_appointments_shopping_list_and_gift_ide
            TextKey.SHOWS_FUTURE_OCCURRENCES_WITH_THE_TASKS_OF_THE_SELECTED_DAY ->
                R.string.msg_shows_future_occurrences_with_the_tasks_of_the_selected_day
            TextKey.SHOWS_OPEN_TASKS_FROM_PAST_DAYS_ON_HOME ->
                R.string.msg_shows_open_tasks_from_past_days_on_home
            TextKey.TIMEZONE_NOT_SUPPORTED -> R.string.msg_timezone_not_supported
            TextKey.FIRST_TASKS -> R.string.msg_first_tasks
            TextKey.JUMP_TO_ANOTHER_DATE -> R.string.msg_jump_to_another_date
            TextKey.ACCESS_TO_DENIED_PLEASE_PUBLISH_THE_CURRENT_FIRESTORE_RULES_AND_TRY_AGAI ->
                R.string
                    .msg_access_to_denied_please_publish_the_current_firestore_rules_and_try_agai
            TextKey.REPLACE_FUTURE_APPOINTMENTS -> R.string.msg_replace_future_appointments
            TextKey.EDITING_REQUIRES_A_CONNECTION -> R.string.msg_editing_requires_a_connection
            TextKey.FOR_EXAMPLE_MILK_BREAD_AND_EGGS_OR_SAY_NEXT_ITEM_BETWEEN_ITEMS ->
                R.string.msg_for_example_milk_bread_and_eggs_or_say_next_item_between_items
            TextKey.TO_SORT_HOLD_THE_HANDLE_AND_DRAG_TAP_IT_FOR_UP_DOWN_ARROWS ->
                R.string.msg_to_sort_hold_the_handle_and_drag_tap_it_for_up_down_arrows
            TextKey.SORTING_REQUIRES_A_CURRENT_SERVER_CONNECTION ->
                R.string.msg_sorting_requires_a_current_server_connection
            TextKey.SORTING_REQUIRES_A_CURRENT_CONNECTION ->
                R.string.msg_sorting_requires_a_current_connection
            TextKey.SAVING_REQUIRES_A_CONNECTION -> R.string.msg_saving_requires_a_connection
            TextKey.SAVING_REQUIRES_A_CURRENT_SERVER_CONNECTION ->
                R.string.msg_saving_requires_a_current_server_connection
            TextKey.MANAGING_LABELS_REQUIRES_A_CURRENT_CONNECTION ->
                R.string.msg_managing_labels_requires_a_current_connection
            TextKey.CURRENT_MONTH -> R.string.msg_current_month
            TextKey.CURRENT_HISTORY -> R.string.msg_current_history
            TextKey.CURRENT_PERIOD -> R.string.msg_current_period
            TextKey.BACK -> R.string.msg_back
            TextKey.BACK_TO_TODAY -> R.string.msg_back_to_today
            TextKey.EVERYDAY_LIFE_GROWS_TOGETHER -> R.string.msg_everyday_life_grows_together
            TextKey.ASSIGNED_TO -> R.string.msg_assigned_to
            TextKey.ASSIGNED_TO_C1E2EE -> R.string.msg_assigned_to_c1e2ee
            TextKey.SHOW -> R.string.msg_show
            TextKey.THIS_AND_ALL_FOLLOWING_OCCURRENCES_8EB5E8 ->
                R.string.msg_this_and_all_following_occurrences_8eb5e8
            TextKey.COMPLETED_TASK -> R.string.msg_completed_task
            TextKey.COMPLETED_TASKS_319F86 -> R.string.msg_completed_tasks_319f86
            TextKey.DELETED_D22871 -> R.string.msg_deleted_d22871
            TextKey.CHANGED_132B10 -> R.string.msg_changed_132b10
            TextKey.ONLY_THIS_OCCURRENCE_BEA118 -> R.string.msg_only_this_occurrence_bea118
            TextKey.REOPENED -> R.string.msg_reopened
            TextKey.REOPEN_3CC598 -> R.string.msg_reopen_3cc598
            TextKey.OLDER_APPOINTMENT_EVENTS_MAY_BE_INCOMPLETE ->
                R.string.msg_older_appointment_events_may_be_incomplete
            TextKey.THE_CHANGE_FAILED_PLEASE_CHECK_YOUR_CONNECTION_AND_RULES ->
                R.string.msg_the_change_failed_please_check_your_connection_and_rules
            TextKey.APPLY_CHANGE_TO -> R.string.msg_apply_change_to
            TextKey.APPLIES_TO_CONFIRMATIONS_AND_STARTED_RECURRING_OCCURRENCES_ARE_PRESERVED ->
                R.string
                    .msg_applies_to_confirmations_and_started_recurring_occurrences_are_preserved
            TextKey.THE_CHANGE_COULD_NOT_BE_SAVED_PLEASE_CHECK_YOUR_CONNECTION_AND_ACCESS_RU ->
                R.string
                    .msg_the_change_could_not_be_saved_please_check_your_connection_and_access_ru
            TextKey.CHANGES_TRASH -> R.string.msg_changes_trash
            TextKey.CHANGES_APPLY_TO_UPCOMING_UNCONFIRMED_OCCURRENCES_STARTED_OCCURRENCES_AN ->
                R.string
                    .msg_changes_apply_to_upcoming_unconfirmed_occurrences_started_occurrences_an
            TextKey.SYNCHRONIZING_CHANGES -> R.string.msg_synchronizing_changes
            TextKey.ABOUT_KINDERALLTAG -> R.string.msg_about_kinderalltag
            TextKey.OVERDUE -> R.string.msg_overdue
            TextKey.SKIP -> R.string.msg_skip
            TextKey.STAYS_IN_THE_HISTORY_YOU_CAN_RESUME_THE_TASK_LATER ->
                R.string.msg_stays_in_the_history_you_can_resume_the_task_later
            TextKey.IS_KEPT_IN_TRASH_PURCHASE_STATUS_AND_HISTORY_ARE_PRESERVED ->
                R.string.msg_is_kept_in_trash_purchase_status_and_history_are_preserved
            TextKey.BECOMES_VISIBLE_AGAIN_WITH_ITS_CURRENT_PURCHASE_STATUS ->
                R.string.msg_becomes_visible_again_with_its_current_purchase_status
            TextKey.BECOMES_VISIBLE_AGAIN_USING_THE_CURRENT_SCHEDULE_NEWER_CHANGES_AND_OTHER ->
                R.string
                    .msg_becomes_visible_again_using_the_current_schedule_newer_changes_and_other
            TextKey.NO_END_DATE -> R.string.msg_no_end_date
            TextKey.NEWEST_FIRST -> R.string.msg_newest_first
            TextKey.DICTATE -> R.string.msg_dictate
            TextKey.REMOVE -> R.string.msg_remove
            TextKey.OPEN_50DD9F -> R.string.msg_open_50dd9f
            TextKey.MOVE -> R.string.msg_move
            TextKey.VALUE_1_DAY_BEFORE -> R.string.msg_value_1_day_before
            TextKey.MORE -> R.string.msg_more
            TextKey.SIGN_OUT -> R.string.msg_sign_out
            TextKey.ENABLE -> R.string.msg_enable
            TextKey.EVERYTHING_IS_UP_TO_DATE -> R.string.msg_everything_is_up_to_date
            TextKey.MARK_AS_BOUGHT -> R.string.msg_mark_as_bought
            TextKey.THE_DAY_BEFORE_AT -> R.string.msg_the_day_before_at
            TextKey.THE_SAME_DAY_AT -> R.string.msg_the_same_day_at
            TextKey.ON_WEEKDAYS -> R.string.msg_on_weekdays
            TextKey.MANAGE_ANDROID_NOTIFICATIONS -> R.string.msg_manage_android_notifications
            TextKey.ARCHIVE -> R.string.msg_archive
            TextKey.AT_A_GLANCE -> R.string.msg_at_a_glance
            TextKey.TASK_MANAGEMENT -> R.string.msg_task_management
            TextKey.FROM_AVAILABLE_DATA -> R.string.msg_from_available_data
            TextKey.RECONSTRUCTED_FROM_AVAILABLE_DATA ->
                R.string.msg_reconstructed_from_available_data
            TextKey.START -> R.string.msg_start
            TextKey.AT_START -> R.string.msg_at_start
            TextKey.NOTIFICATIONS -> R.string.msg_notifications
            TextKey.AREA -> R.string.msg_area
            TextKey.BOUGHT_ITEMS -> R.string.msg_bought_items
            TextKey.UPCOMING -> R.string.msg_upcoming
            TextKey.REFERENCE_DAY -> R.string.msg_reference_day
            TextKey.APPEARANCE -> R.string.msg_appearance
            TextKey.THE_HOME_SCREEN_APP_ICON_CHANGES_WITH_THE_THEME_YOUR_LAUNCHER_MAY_TAKE_A ->
                R.string
                    .msg_the_home_screen_app_icon_changes_with_the_theme_your_launcher_may_take_a
            TextKey.THE_END_DATE_IS_BEFORE_THE_START_DATE ->
                R.string.msg_the_end_date_is_before_the_start_date
            TextKey.THE_START_DATE_DETERMINES_THE_RHYTHM ->
                R.string.msg_the_start_date_determines_the_rhythm
            TextKey.THE_SERIES_BEGINS_BEFORE_THE_RECURRENCE_STARTS ->
                R.string.msg_the_series_begins_before_the_recurrence_starts
            TextKey.THE_SERIES_START_DOES_NOT_MATCH_THE_INTERVAL ->
                R.string.msg_the_series_start_does_not_match_the_interval
            TextKey.THEME -> R.string.msg_theme
            TextKey.THIS_LABEL_ALREADY_EXISTS -> R.string.msg_this_label_already_exists
            TextKey.DUPLICATE_RECURRENCE_FIELDS -> R.string.msg_duplicate_recurrence_fields
            TextKey.PHARMACY_TOILETRIES -> R.string.msg_pharmacy_toiletries
            TextKey.SEND_EMAIL -> R.string.msg_send_email
            TextKey.SHOPPING_ITEM -> R.string.msg_shopping_item
            TextKey.SHOPPING_ITEM_ADDED -> R.string.msg_shopping_item_added
            TextKey.SHOPPING_ITEM_BOUGHT -> R.string.msg_shopping_item_bought
            TextKey.SHOPPING_ITEM_RESTORED -> R.string.msg_shopping_item_restored
            TextKey.SHOPPING_LABELS -> R.string.msg_shopping_labels
            TextKey.EDIT_ENTRY -> R.string.msg_edit_entry
            TextKey.PARENTS -> R.string.msg_parents
            TextKey.END_DATE_OPTIONAL -> R.string.msg_end_date_optional
            TextKey.END_SAME_DAY -> R.string.msg_end_same_day
            TextKey.EVENTS -> R.string.msg_events
            TextKey.COMPLETED -> R.string.msg_completed
            TextKey.TRY_AGAIN -> R.string.msg_try_again
            TextKey.REPLACE -> R.string.msg_replace
            TextKey.YOUR_FAMILY_DAY_IN_NUMBERS -> R.string.msg_your_family_day_in_numbers
            TextKey.YOUR_EVERYDAY_HELPER -> R.string.msg_your_everyday_helper
            TextKey.YOUR_PROGRESS -> R.string.msg_your_progress
            TextKey.YOUR_FAMILY_GARDEN -> R.string.msg_your_family_garden
            TextKey.YOUR_DAILY_RHYTHM -> R.string.msg_your_daily_rhythm
            TextKey.FAMILY_TIMEZONE_EUROPE_BERLIN -> R.string.msg_family_timezone_europe_berlin
            TextKey.FAMILY_ACCESS -> R.string.msg_family_access
            TextKey.COLOUR -> R.string.msg_colour
            TextKey.EVENT_COLOURS -> R.string.msg_event_colours
            TextKey.FIREBASE_SIGN_IN -> R.string.msg_firebase_sign_in
            TextKey.BIRTHDAY -> R.string.msg_birthday
            TextKey.BOUGHT_GIFTS -> R.string.msg_bought_gifts
            TextKey.YELLOW -> R.string.msg_yellow
            TextKey.ON_AN_EVERYDAY_MISSION_TOGETHER -> R.string.msg_on_an_everyday_mission_together
            TextKey.ROLLING_THROUGH_EVERYDAY_LIFE_TOGETHER ->
                R.string.msg_rolling_through_everyday_life_together
            TextKey.GROWING_THROUGH_YOUR_DAY_TOGETHER ->
                R.string.msg_growing_through_your_day_together
            TextKey.REAPING_THE_REWARDS_OF_FAMILY_LIFE_TOGETHER ->
                R.string.msg_reaping_the_rewards_of_family_life_together
            TextKey.SHARED_LABELS_OPTIONAL -> R.string.msg_shared_labels_optional
            TextKey.PLANNED -> R.string.msg_planned
            TextKey.ENTIRE_SERIES -> R.string.msg_entire_series
            TextKey.GIFT_IDEA -> R.string.msg_gift_idea
            TextKey.GIFT_IDEA_ADDED -> R.string.msg_gift_idea_added
            TextKey.GIFT_IDEA_BOUGHT -> R.string.msg_gift_idea_bought
            TextKey.GIFT_IDEA_RESTORED -> R.string.msg_gift_idea_restored
            TextKey.SAVED_DATA -> R.string.msg_saved_data
            TextKey.IF_THE_DAY_DOES_NOT_EXIST_IN_A_MONTH_THE_LAST_DAY_OF_THE_MONTH_IS_USED ->
                R.string.msg_if_the_day_does_not_exist_in_a_month_the_last_day_of_the_month_is_used
            TextKey.GOOGLE_CREDENTIALS -> R.string.msg_google_credentials
            TextKey.GOOGLE_SIGN_IN_FAILED -> R.string.msg_google_sign_in_failed
            TextKey.GOOGLE_ACCOUNT_SELECTION -> R.string.msg_google_account_selection
            TextKey.PUBLISHER_CONTACT -> R.string.msg_publisher_contact
            TextKey.SAVE_TO_CALENDAR -> R.string.msg_save_to_calendar
            TextKey.LEGAL_NOTICE -> R.string.msg_legal_notice
            TextKey.INTERVAL_1_TO_1000 -> R.string.msg_interval_1_to_1000
            TextKey.NO_END_DATE_08A7C2 -> R.string.msg_no_end_date_08a7c2
            TextKey.KINDERALLTAG -> R.string.msg_kinderalltag
            TextKey.PAEDIATRICIAN -> R.string.msg_paediatrician
            TextKey.CHILD_PROFILE -> R.string.msg_child_profile
            TextKey.CHILD_PROFILES -> R.string.msg_child_profiles
            TextKey.LITTLE_STEPS_THAT_CARRY_YOUR_FAMILY_DAY ->
                R.string.msg_little_steps_that_carry_your_family_day
            TextKey.CONFIGURATION -> R.string.msg_configuration
            TextKey.EDIT_LABEL -> R.string.msg_edit_label
            TextKey.LABEL_1_TO_60_CHARACTERS -> R.string.msg_label_1_to_60_characters
            TextKey.LABELS -> R.string.msg_labels
            TextKey.MANAGE_LABELS -> R.string.msg_manage_labels
            TextKey.DICTATE_LIST -> R.string.msg_dictate_list
            TextKey.LIST_LABELS -> R.string.msg_list_labels
            TextKey.LIST_HISTORY -> R.string.msg_list_history
            TextKey.MAGICAL_WORLD -> R.string.msg_magical_world
            TextKey.SOME_IDEAS_JUST_NEED_A_HOME_UNTIL_THE_RIGHT_MOMENT_TO_GIVE_THEM ->
                R.string.msg_some_ideas_just_need_a_home_until_the_right_moment_to_give_them
            TextKey.SIGN_IN_WITH_GOOGLE -> R.string.msg_sign_in_with_google
            TextKey.MONTH -> R.string.msg_month
            TextKey.MONTHLY_TRACKER -> R.string.msg_monthly_tracker
            TextKey.BY_AREA -> R.string.msg_by_area
            TextKey.FILTER_BY_LABELS -> R.string.msg_filter_by_labels
            TextKey.BY_WEEKDAY -> R.string.msg_by_weekday
            TextKey.MOVE_UP -> R.string.msg_move_up
            TextKey.MOVE_DOWN -> R.string.msg_move_down
            TextKey.NAME -> R.string.msg_name
            TextKey.NEW_GIFT_IDEA -> R.string.msg_new_gift_idea
            TextKey.NEW_SHOPPING_ITEM -> R.string.msg_new_shopping_item
            TextKey.NEW_LABEL -> R.string.msg_new_label
            TextKey.FAMILY_UPDATES -> R.string.msg_family_updates
            TextKey.EVENTS_NOT_YET_SYNCHRONIZED -> R.string.msg_events_not_yet_synchronized
            TextKey.STILL_OPEN -> R.string.msg_still_open
            TextKey.GRANDMA -> R.string.msg_grandma
            TextKey.GRANDPA -> R.string.msg_grandpa
            TextKey.EASTER -> R.string.msg_easter
            TextKey.PAUSED -> R.string.msg_paused
            TextKey.GIFT_GIVERS -> R.string.msg_gift_givers
            TextKey.PLAYING_THROUGH_YOUR_DAY_TOGETHER ->
                R.string.msg_playing_through_your_day_together
            TextKey.START_DATE -> R.string.msg_start_date
            TextKey.START_DATE_MISSING -> R.string.msg_start_date_missing
            TextKey.HOME -> R.string.msg_home
            TextKey.STATUS -> R.string.msg_status
            TextKey.SUPERMARKET -> R.string.msg_supermarket
            TextKey.SYNCHRONIZED -> R.string.msg_synchronized
            TextKey.SYNCHRONIZATION_PENDING -> R.string.msg_synchronization_pending
            TextKey.DAY -> R.string.msg_day
            TextKey.APPOINTMENT_CANCELLATIONS -> R.string.msg_appointment_cancellations
            TextKey.APPOINTMENT_HISTORY -> R.string.msg_appointment_history
            TextKey.AN_ANIMAL_ADVENTURE_THROUGH_YOUR_DAY_TOGETHER ->
                R.string.msg_an_animal_adventure_through_your_day_together
            TextKey.TITLE -> R.string.msg_title
            TextKey.TITLE_1_TO_120_CHARACTERS -> R.string.msg_title_1_to_120_characters
            TextKey.CREATE_ANOTHER_COPY_ANYWAY -> R.string.msg_create_another_copy_anyway
            TextKey.TIMES -> R.string.msg_times
            TextKey.PLEASE_SPECIFY_TIMES_WITH_MINUTE_PRECISION ->
                R.string.msg_please_specify_times_with_minute_precision
            TextKey.UNKNOWN -> R.string.msg_unknown
            TextKey.VERSION -> R.string.msg_version
            TextKey.PREVIOUS_WEEK -> R.string.msg_previous_week
            TextKey.PREVIOUS_MONTH -> R.string.msg_previous_month
            TextKey.PREVIOUS_DAY -> R.string.msg_previous_day
            TextKey.PREVIEW_IN_DAYS -> R.string.msg_preview_in_days
            TextKey.CHRISTMAS -> R.string.msg_christmas
            TextKey.MORE_196A0A -> R.string.msg_more_196a0a
            TextKey.RESTORE -> R.string.msg_restore
            TextKey.RESTORE_ABD462 -> R.string.msg_restore_abd462
            TextKey.SAVING_6B432E -> R.string.msg_saving_6b432e
            TextKey.WEEK -> R.string.msg_week
            TextKey.PERIOD -> R.string.msg_period
            TextKey.DEFAULT_WEB_CLIENT_ID_IS_MISSING_FROM_THE_FIREBASE_CONFIGURATION ->
                R.string.msg_default_web_client_id_is_missing_from_the_firebase_configuration
            TextKey.LANGUAGE -> R.string.msg_language
            TextKey.SYSTEM_LANGUAGE -> R.string.msg_system_language
            TextKey.ARCHIVED -> R.string.msg_archived
            TextKey.ME -> R.string.msg_me
            TextKey.UNTIL_1_S -> R.string.msg_until_1_s
            TextKey.PAUSED_91AB11 -> R.string.msg_paused_91ab11
            TextKey.RESTORED -> R.string.msg_restored
            TextKey.PENDING -> R.string.msg_pending
            TextKey.TODAY_638B36 -> R.string.msg_today_638b36
            TextKey.EVENT -> R.string.msg_event
            TextKey.COMPLETED_DETAILS -> R.string.msg_completed_details
            TextKey.MARK_AS_BOUGHT_5ACF70 -> R.string.msg_mark_as_bought_5acf70
            TextKey.ADDED -> R.string.msg_added
            TextKey.HIDE -> R.string.msg_hide
            TextKey.BOUGHT_1F118D -> R.string.msg_bought_1f118d
            TextKey.RESTORED_9B1B84 -> R.string.msg_restored_9b1b84
        }

internal val QuantityKey.resourceId: Int
    get() =
        when (this) {
            QuantityKey.TASK_PROGRESS -> R.plurals.task_progress
            QuantityKey.APPOINTMENT_PROGRESS -> R.plurals.appointment_progress
            QuantityKey.IMPORT_QUEUED -> R.plurals.import_queued
            QuantityKey.DAY_EVENTS -> R.plurals.day_events
        }
