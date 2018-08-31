package com.ognev.kotlin.agendacalendarview.sample

import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.ognev.kotlin.agendacalendarview.CalendarController
import com.ognev.kotlin.agendacalendarview.builder.CalendarContentManager
import com.ognev.kotlin.agendacalendarview.models.CalendarEvent
import com.ognev.kotlin.agendacalendarview.models.DayItem
import com.ognev.kotlin.agendacalendarview.models.IDayItem
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity(), CalendarController {

    private var monthEventsList: MutableList<CalendarEvent> = arrayListOf()
    private lateinit var minDate: Calendar
    private lateinit var maxDate: Calendar
    private lateinit var contentManager: CalendarContentManager
    private var startMonth: Int = Calendar.getInstance().get(Calendar.MONTH)
    private var endMonth: Int = Calendar.getInstance().get(Calendar.MONTH)

    private var eventsForYearPerMonth: MutableMap<Int, List<CalendarEvent>> = HashMap(12)

    private var loadingTask: LoadingTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        minDate = Calendar.getInstance()
        maxDate = Calendar.getInstance()

        minDate.add(Calendar.MONTH, -10)
        minDate.add(Calendar.YEAR, -1)
        minDate.set(Calendar.DAY_OF_MONTH, 1)
        maxDate.add(Calendar.YEAR, 1)

        contentManager = CalendarContentManager(this, agenda_calendar_view, SampleEventAgendaAdapter(applicationContext))

        contentManager.locale = Locale.ENGLISH
        contentManager.setDateRange(minDate, maxDate)


        val maxLength = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)

        for (i in 1..maxLength) {
            val day = Calendar.getInstance(Locale.ENGLISH)
            day.timeInMillis = System.currentTimeMillis()
            day.set(Calendar.DAY_OF_MONTH, i)

            monthEventsList.add(MyCalendarEvent(day, day,
                    DayItem.buildDayItemFromCal(day), null).setEventInstanceDay(day))
        }

        contentManager.loadItemsFromStart(monthEventsList)
        eventsForYearPerMonth[startMonth] = monthEventsList
    }

    override fun onStop() {
        super.onStop()
        loadingTask?.cancel(true)
    }

    override fun getEmptyEventLayout() = R.layout.view_agenda_empty_event

    override fun getEventLayout() = R.layout.view_agenda_event

    override fun onDaySelected(dayItem: IDayItem) {
        val selected = Calendar.getInstance(Locale.getDefault())
        selected.time = dayItem.date
        if (!eventsForYearPerMonth.containsKey(selected.get(Calendar.MONTH))){
            loadItemsForMonth(selected)
        }
    }

    override fun onScrollToDate(calendar: Calendar) {

        if (afterAddEnter){
            afterAddEnter = false
            return
        }

        if (calendar.get(Calendar.DAY_OF_MONTH) == 1) {
            if (eventsForYearPerMonth[calendar.get(Calendar.MONTH)-1] != null){
                return
            }
            if (!agenda_calendar_view.isCalendarLoading()) {
                loadItemsAsync(true)
            }
        }

        if (calendar.get(Calendar.DAY_OF_MONTH) > 24) {
            if (eventsForYearPerMonth[calendar.get(Calendar.MONTH)+1] != null){
                return
            }
            if (!agenda_calendar_view.isCalendarLoading()) {
                loadItemsAsync(false)
            }
        }

    }

    private fun loadItemsAsync(addFromStart: Boolean) {
        loadingTask?.cancel(true)

        loadingTask = LoadingTask(addFromStart)
        loadingTask?.execute()
    }

    private var afterAddEnter = false

    inner class LoadingTask(private val addFromStart: Boolean) : AsyncTask<Unit, Unit, Int>() {

        private val startMonthCal: Calendar = Calendar.getInstance()
        private val endMonthCal: Calendar = Calendar.getInstance()

        override fun onPreExecute() {
            super.onPreExecute()
            agenda_calendar_view.showProgress()
            monthEventsList.clear()
        }

        override fun doInBackground(vararg params: Unit?): Int {

            if (addFromStart) {
                if (startMonth == 0) {// If month is January - Make it december
                    startMonth = 11
                } else {// else decrement it by 1
                    startMonth--
                }

                startMonthCal.set(Calendar.MONTH, startMonth)// Set new start month to add events
                if (startMonth == 11) {// Check if the month is made to december and increase year
                    var year = startMonthCal.get(Calendar.YEAR)
                    startMonthCal.set(Calendar.YEAR, ++year)
                }


                for (i in 1..startMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)) {// Add events for each day
                    val day = Calendar.getInstance(Locale.ENGLISH)
                    day.timeInMillis = System.currentTimeMillis()
                    day.set(Calendar.MONTH, startMonth)
                    day.set(Calendar.DAY_OF_MONTH, i)
                    if (endMonth == 11) {
                        day.set(Calendar.YEAR, day.get(Calendar.YEAR) - 1)
                    }

                    monthEventsList.add(MyCalendarEvent(day, day,
                            DayItem.buildDayItemFromCal(day),
                            SampleEvent(name = "Awesome $i", description = "Event $i"))
                            .setEventInstanceDay(day))
                }
                return startMonth
            } else {
                if (endMonth >= 11) {
                    endMonth = 0
                } else {
                    endMonth++
                }

                endMonthCal.set(Calendar.MONTH, endMonth)
                if (endMonth == 0) {
                    var year = endMonthCal.get(Calendar.YEAR)
                    endMonthCal.set(Calendar.YEAR, ++year)
                }

                for (i in 1..endMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)) {
                    val day = Calendar.getInstance(Locale.ENGLISH)
                    day.timeInMillis = System.currentTimeMillis()
                    day.set(Calendar.MONTH, endMonth)
                    day.set(Calendar.DAY_OF_MONTH, i)
                    if (endMonth == 0) {
                        day.set(Calendar.YEAR, day.get(Calendar.YEAR) + 1)
                    }

                    if (i % 4 == 0) {
                        val day1 = Calendar.getInstance()
                        day1.timeInMillis = System.currentTimeMillis()
                        day1.set(Calendar.MONTH, endMonth)
                        day1.set(Calendar.DAY_OF_MONTH, i)
                        monthEventsList.add(MyCalendarEvent(day, day,
                                DayItem.buildDayItemFromCal(day),
                                SampleEvent(name = "Awesome $i, inner", description = "Event $i"))
                                .setEventInstanceDay(day))
                    }

                    monthEventsList.add(MyCalendarEvent(day, day,
                            DayItem.buildDayItemFromCal(day),
                            SampleEvent(name = "Awesome $i", description = "Event $i"))
                            .setEventInstanceDay(day))
                }
                return endMonth
            }
        }

        override fun onPostExecute(month: Int) {
            val temp = Calendar.getInstance()
            val tempDay = temp.getActualMaximum(Calendar.MONTH)
            temp.set(Calendar.MONTH, month)
            temp.set(Calendar.DAY_OF_MONTH, tempDay-6)

            if (addFromStart) {
                contentManager.loadItemsFromStart(monthEventsList)
            } else {
                contentManager.loadFromEndCalendar(monthEventsList)
            }
            afterAddEnter = true
            agenda_calendar_view.hideProgress()
            eventsForYearPerMonth[month] = monthEventsList
        }
    }

    private fun loadItemsForMonth(selectedCal: Calendar) {
        monthEventsList.clear()

        for (i in 1..selectedCal.getActualMaximum(Calendar.DAY_OF_MONTH)) {// Add events for each day
            val day = Calendar.getInstance(Locale.ENGLISH)
            day.timeInMillis = System.currentTimeMillis()
            day.set(Calendar.MONTH, selectedCal.get(Calendar.MONTH))
            day.set(Calendar.DAY_OF_MONTH, i)
            monthEventsList.add(MyCalendarEvent(day, day,
                    DayItem.buildDayItemFromCal(day),
                    SampleEvent(name = "Awesome $i", description = "Event $i"))
                    .setEventInstanceDay(day))
        }
        contentManager.loadItemsFromStart(monthEventsList)
        eventsForYearPerMonth[selectedCal.get(Calendar.MONTH)] = monthEventsList
        afterAddEnter = true
        agenda_calendar_view.agendaView.agendaListView.scrollToCurrentDate(selectedCal)
    }
}
