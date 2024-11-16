package com.codelama.weather_app;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;


public class Widget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);


            views.setTextViewText(R.id.widget_temperature, "N/A");
            views.setImageViewResource(R.id.widget_weather_icon, R.drawable.w01d);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

}