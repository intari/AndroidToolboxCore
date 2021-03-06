package net.intari.AndroidToolboxCore;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.view.ContextThemeWrapper;

import com.amplitude.api.Amplitude;
import com.amplitude.api.Identify;
import com.yandex.metrica.Revenue;
import com.yandex.metrica.YandexMetrica;
import com.yandex.metrica.YandexMetricaConfig;
import com.yandex.metrica.profile.Attribute;
import com.yandex.metrica.profile.BirthDateAttribute;
import com.yandex.metrica.profile.BooleanAttribute;
import com.yandex.metrica.profile.GenderAttribute;
import com.yandex.metrica.profile.NameAttribute;
import com.yandex.metrica.profile.NumberAttribute;
import com.yandex.metrica.profile.StringAttribute;
import com.yandex.metrica.profile.UserProfile;


import net.intari.AndroidToolboxCore.observers.ActivityLifecycleObserver;
import net.intari.CustomLogger.CustomLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
 * Support utils for my projects
 * (c) Dmitriy Kazimirov 2015-2018, e-mail:dmitriy.kazimirov@viorsan.com
 *
 *
 */
public class CoreUtils {
    public static final String TAG = CoreUtils.class.getSimpleName();

    public  enum Gender {
        NOT_KNOWN,
        MALE,
        FEMALE,
        OTHER
    }


    public  enum ReportLifecycleAs {
        JUST_EVENT,
        PREFIXED_SHORT_CLASS_NAME,
        PREFIXED_FULL_CLASS_NAME,
    }

    public static final int NO_AGE=-1;
    public static final int NO_DOB_YEAR=-1;
    public static final int NO_DOB_MONTH=-1;
    public static final int NO_DOB_DAY=-1;
    public static final int NO_DOB_CALENDAR=-1;

    public static final String NAME="name";
    public static final String AGE="age";
    public static final String DOB="Day of Birth";
    public static final String GENDER="gender";
    public static final String USERNAME="username";


    //per https://stackoverflow.com/questions/880365/any-way-to-invoke-a-private-method
    //does NOT work with @hide methods as of Android 9
    public static Object genericInvokMethod(Object obj, String methodName,
                                            int paramCount, Object... params) {
        Method method;
        Object requiredObj = null;
        Object[] parameters = new Object[paramCount];
        Class<?>[] classArray = new Class<?>[paramCount];
        for (int i = 0; i < paramCount; i++) {
            parameters[i] = params[i];
            classArray[i] = params[i].getClass();
        }
        try {
            method = obj.getClass().getDeclaredMethod(methodName, classArray);
            method.setAccessible(true);
            requiredObj = method.invoke(obj, params);
        } catch (NoSuchMethodException e) {
            CustomLog.logException(e);
        } catch (IllegalArgumentException e) {
            CustomLog.logException(e);
        } catch (IllegalAccessException e) {
            CustomLog.logException(e);
        } catch (InvocationTargetException e) {
            CustomLog.logException(e);
        }

        return requiredObj;
    }

    //add prefix to every map element
    public static Map<String,String> addPrefixToMap(Map<String,String> map,String prefix) {
        Map<String,String> result=new HashMap<>();
        for (String key:map.keySet()) {
            result.put(prefix+key,map.get(key));
        }
        return result;
    }

    /*
      ConvertS JSON to form which can be used as params for Volley HTTP posting
      Supported types:JSONObject, JSONArray,Integer,Long,Double,String
      .toString is called for everything else
      Exception handling is caller's responsibility
     */
    public static Map<String,String> encodeJSONToMapWithPrefix(JSONObject json,String prefix2) throws JSONException, UnsupportedEncodingException {
        Map<String,String> result=new HashMap<>();
        Iterator<String> keys = json.keys();
        String keyPrefix="";
        if (prefix2!=null) {
            keyPrefix=prefix2;
        }
        while (keys.hasNext()) {
            String key = keys.next();
            Object value= json.get(key);

            if (value instanceof JSONObject) {
                Map<String, String> r = encodeJSONToMapWithPrefix((JSONObject) value, keyPrefix + "[" + key + "]");
                result.putAll(r);

            } else if (value instanceof JSONArray) {
                JSONArray jarr=(JSONArray)value;
                for (int i=0;i<jarr.length();i++) {
                    Map<String, String> r = encodeJSONToMapWithPrefix(jarr.getJSONObject(i), keyPrefix + "[" + key + "][]");
                    result.putAll(r);
                }
            } else if (value instanceof Integer) {
                result.put(keyPrefix+"["+key+"]",Integer.toString((Integer)value));
            } else if (value instanceof Long) {
                result.put(keyPrefix+"["+key+"]",Long.toString((Long)value));
            } else if (value instanceof Double) {
                result.put(keyPrefix+"["+key+"]",Double.toString((Double)value));
            } else if (value instanceof String) {
                result.put(keyPrefix+"["+key+"]",(String)value);
            } else {
                result.put(keyPrefix+"["+key+"]",value.toString());
            }


        }
        return result;

    }

    public static int dpToPx(int dp)
    {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density );
    }

    public static int pxToDp(int px)
    {
        return (int) (px / Resources.getSystem().getDisplayMetrics().density);
    }
    //thread utils
    public static long getThreadId()
    {
        Thread t = Thread.currentThread();
        return t.getId();
    }

    public static String getThreadSignature()
    {
        Thread t = Thread.currentThread();
        long l = t.getId();
        String name;
        if (CoreUtils.isUiThread()) {
            name="(UI)"+ t.getName();
        } else {
            name= t.getName();
        }
        long p = t.getPriority();
        String gname = t.getThreadGroup().getName();
        return (name
                + ":(id)" + l
                + ":(priority)" + p
                + ":(group)" + gname);
    }

    /**
     * Is this UI Thread?
     * @return
     */
    public static boolean isUiThread()
    {
        return Looper.getMainLooper().equals(Looper.myLooper());
    }

    private static  Map<String, Object> superAttributes = new HashMap<>();

    private static boolean analytics_YandexMetricaActive = false;

    private static boolean analytics_AmplitudeActive = false;


    private static int resumed;
    private static int paused;
    private static int started;
    private static int stopped;

    public static boolean isApplicationVisible() {
        return started > stopped;
    }

    public static boolean isApplicationInForeground() {
        return resumed > paused;
    }


    private static long screenStart=0L;

    public static void reportScreenStart(String screenName) {
        screenStart = System.currentTimeMillis();
        Map<String,Object> eventProperties=new HashMap<>();
        eventProperties.put("screenName",screenName);
        reportAnalyticsEvent("screenStart",eventProperties);
    }

    public static void reportScreenStop(String screenName) {
        long screenStop = System.currentTimeMillis();
        long elaspedTime = (screenStop-screenStart)/Constants.MS_PER_SECOND;
        Map<String,Object> eventProperties=new HashMap<>();
        eventProperties.put("screenName",screenName);
        eventProperties.put("timeTook",elaspedTime);
        reportAnalyticsEvent("screenStop",eventProperties);
    }

    private static ActivityLifecycleObserver activityLifecycleObserver;

    private static Application internalApp;
    private static boolean reportLifecycleEventsForDebug=false;

    /**
     * Should observers report lifecycle events to CustomLog?
     * @param report
     */
    public static void setReportLifecycleEventsForDebug(boolean report) {
        reportLifecycleEventsForDebug=report;
    }

    public static boolean isReportLifecycleEventsForDebug() {
        return reportLifecycleEventsForDebug;
    }

    private static boolean reportLifecycleEventsForAnalytics=false;

    public static boolean isReportLifecycleEventsForAnalytics_OnlyStart() {
        return reportLifecycleEventsForAnalytics_OnlyStart;
    }

    /**
     * Show we report only start events and not stop events?
     * @param reportLifecycleEventsForAnalytics_OnlyStart
     */
    public static void setReportLifecycleEventsForAnalytics_OnlyStart(boolean reportLifecycleEventsForAnalytics_OnlyStart) {
        CoreUtils.reportLifecycleEventsForAnalytics_OnlyStart = reportLifecycleEventsForAnalytics_OnlyStart;
    }

    private static boolean reportLifecycleEventsForAnalytics_OnlyStart=false;

    private static ReportLifecycleAs reportLifecycleForAnalyticsAs=ReportLifecycleAs.JUST_EVENT;

    /**
     * How to name analytics events ?
     * @param reportLifecycleAs
     */
    public static void setReportLifecycleForAnalyticsAs(ReportLifecycleAs reportLifecycleAs) {
        reportLifecycleForAnalyticsAs=reportLifecycleAs;
    }

    /**
     * How analytics events are named
     * @return
     */
    public static ReportLifecycleAs getReportLifecycleForAnalyticsAs() {
        return reportLifecycleForAnalyticsAs;
    }

    /**
     * Should observers report lifecycle events to analytics?
     * @param report
     */
    public static void setReportLifecycleEventsForAnalytics(boolean report) {
        reportLifecycleEventsForAnalytics=report;
    }

    public static boolean isReportLifecycleEventsForAnalytics() {
        return reportLifecycleEventsForAnalytics;
    }

    /**
     * Inits lifecycle handlers
     * Will send analytics events
     * @param app
     */
    public static void initLifecycleObservers(Application app) {
        if (activityLifecycleObserver == null) {
            activityLifecycleObserver = new ActivityLifecycleObserver();
            app.registerActivityLifecycleCallbacks(activityLifecycleObserver);
            internalApp=app;
        }
    }

    /**
     * Reverses effects of initLifecycleReporters
     */
    public static void unInitLifecyleListeners() {
        if ((internalApp!=null) && (activityLifecycleObserver!=null)) {
            internalApp.unregisterActivityLifecycleCallbacks(activityLifecycleObserver);
        }
    }


    /**
     * Enable Yandex App Metrica for reportAnalyticsEvent.
     * It's up to client to perform actual initialization and provide keys
     * See
     * https://tech.yandex.ru/appmetrica/doc/mobile-sdk-dg/concepts/android-initialize-docpage/ or
     * @param activate
     */
    public static void activateYandexMetrica(boolean activate) {
        analytics_YandexMetricaActive=activate;
    }

    /**
     * Init Yandex.Metrica
     * see https://tech.yandex.ru/appmetrica/doc/mobile-sdk-dg/concepts/android-initialize-docpage/
     * call activateYandexMetrica after this one
     * @param app
     * @param apiKey
     * @param locationTracking
     * @param firstLaunchIsUpdate
     * @param withCrashReporting
     * @param autoTracking
     */
    public static void initYandexMetrica(Application app,String apiKey,boolean locationTracking,boolean firstLaunchIsUpdate,boolean withCrashReporting,boolean autoTracking) {
        // Инициализация AppMetrica SDK
        YandexMetricaConfig.Builder configBuilder = YandexMetricaConfig.newConfigBuilder(apiKey);
        configBuilder.withCrashReporting(withCrashReporting);
        configBuilder.withLocationTracking(locationTracking);
        configBuilder.handleFirstActivationAsUpdate(firstLaunchIsUpdate);
        YandexMetrica.activate(app.getApplicationContext(), configBuilder.build());
        if (autoTracking) {
            YandexMetrica.enableActivityAutoTracking(app);
        }
    }

    /**
     * Reports throwable to Crashlytics AND yandexMetrica
     * @param throwable
     */
    public static void reportThrowable(Throwable throwable) {
        CustomLog.logException(throwable);
        yandexMetricaReportCrash(throwable);
    }

    /**
     * Report crash to Yandex.Metrica
     * @param throwable
     */
    public static void yandexMetricaReportCrash(Throwable throwable) {
        if (analytics_YandexMetricaActive) {
            try {
                YandexMetrica.reportUnhandledException(throwable);
            } catch (Exception e) {
                CustomLog.logException(e);
            }
        }
    }
    /**
     * Init Amplitude and enable foreground tracking
     * https://developers.amplitude.com/?java#installation
     * Call this one and call activateAmplitude(true) after it
     * @param context
     * @param apiKey
     * @param application
     */
    public static void initAmplitude(Context context, String apiKey, Application application) {
        Amplitude.getInstance().trackSessionEvents(true);
        Amplitude.getInstance().initialize(context,apiKey)
                .enableForegroundTracking(application);
    }
    /**
     * Enable Amplitude for reportAnalyticsEvent.
     * It's up to client to perform actual initialization and provide keys
     * See https://amplitude.zendesk.com/hc/en-us/articles/115002935588#installation
     * @param activate
     */
    public static void activateAmplitude(boolean activate) {
        analytics_AmplitudeActive=activate;
    }

    /**
     * Adds 'super attribute' to be sent with each report (or replace one if it's exist)
     * @param key attribute name
     * @param obj attribute value
     */
    public static void addAnalyticsSuperAttribute(String key,Object obj) {
        superAttributes.put(key,obj);

    }
    /**
     * Report event to analytics
     * @param event event name
     */
    public static void reportAnalyticsEvent(String event) {
        reportAnalyticsEvent(event,null);
    }


    /**
     * Report event to analytics
     * It's assumed that analytics libs are initialized
     * @param event event name
     * @param eventAttributes - Map<String, Object> attributes to send with event
     *
     */
    public static void reportAnalyticsEvent(String event, Map<String, Object> eventAttributes) {
        //add super attributes
        Map<String, Object> attributes=new HashMap<>();
        if (eventAttributes!=null) {
            attributes.putAll(eventAttributes);
        }
        attributes.putAll(superAttributes);

        //send event to  Yandex.Metrica
        if (analytics_YandexMetricaActive) {
            YandexMetrica.reportEvent(event,attributes);
        }
        //Convert attributes for Amplitude and Mixpanel
        try {
            JSONObject props = new JSONObject();
            for (String key:attributes.keySet()) {
                props.put(key,attributes.get(key));
            }

            if (analytics_AmplitudeActive) {
                Amplitude.getInstance().logEvent(event,props);
            }

            //WARNING!. Mixpanel has rather low free limits!
            //AppController.getInstance().getMixpanel().track(event);
        }  catch (JSONException e) {
            CustomLog.logException(e);
        }
        //TODO: also write to (encrypted) log file
    }


    /**
     * Report event to analytics but do it NOW (and block main thread
     * It's assumed that analytics libs are initialized
     * @param event event name
     * @param eventAttributes attributes to send with event
     *
     */
    public static void reportAnalyticsEventSync(String event, Map<String, Object> eventAttributes) {
        //add super attributes
        Map<String, Object> attributes=new HashMap<String, Object>();
        if (eventAttributes!=null) {
            attributes.putAll(eventAttributes);
        }
        attributes.putAll(superAttributes);

        //send event to  Yandex.Metrica
        if (analytics_YandexMetricaActive) {
            YandexMetrica.reportEvent(event,attributes);
        }
        //Convert attributes for Amplitude and Mixpanel
        try {
            JSONObject props = new JSONObject();
            for (String key:attributes.keySet()) {
                props.put(key,attributes.get(key));
            }

            if (analytics_AmplitudeActive) {
                Amplitude.getInstance().logEventSync(event,props);
            }

            //WARNING!. Mixpanel has rather low free limits!
            //AppController.getInstance().getMixpanel().track(event);
        }  catch (JSONException e) {
            CustomLog.logException(e);
        }
        //TODO: also write to (encrypted) log file
    }
    /**
     * Reports YandexMetrica's userProfile
     * @see https://tech.yandex.ru/appmetrica/doc/mobile-sdk-dg/concepts/android-methods-docpage/
     * @param userProfile
     */
    public static void reportYandexUserProfile(com.yandex.metrica.profile.UserProfile userProfile) {
        if (analytics_YandexMetricaActive) {
            try {
                YandexMetrica.reportUserProfile(userProfile);
            } catch (Exception e) {
                CustomLog.logException(e);
            }
        }
    }


    /**
     * Report profile
     * only WithValue supported for yandex(not WithValueIfUndefined)
     * @param userAttributes
     * @param name - value of special attribute 'name', (could be null)
     * @param gender - value of special attribute 'gender' (use Gender.Gender.NOT_KNOWN if none known)
     * @param age - value of special attribute 'age' (use NO_AGE if none known)
     * @param dob_year - value of special attribute 'year of birth' (use NO_DOB_YEAR if none known)
     * @param dob_month - value of special attribute 'month of birth' (use NO_DOB_MONTH if none known)
     * @param dob_day - value of special attribute 'year of birth' (use NO_DOB_DAY if none known)
     * @param dob_calendar - value of special attribute 'calendar' (used as way to get date of birth) (use null if none known)
     */
    public static void reportProfile(Map<String, Object> userAttributes, String name, Gender gender,
                                     int age, int dob_year, int dob_month, int dob_day, Calendar dob_calendar) {

        //TODO:age/dob support
        //TODO:put name/gender to amplitude too as regular attributes


        if (analytics_YandexMetricaActive) {
            UserProfile.Builder builder=com.yandex.metrica.profile.UserProfile.newBuilder();
            for (String key:userAttributes.keySet()) {
                Object obj=userAttributes.get(key);
                if (obj==null) {
                    StringAttribute stringAttribute= Attribute.customString(key);
                    builder.apply(stringAttribute.withValue("<null>"));
                } else  if (obj instanceof String) {
                    StringAttribute stringAttribute= Attribute.customString(key);
                    builder.apply(stringAttribute.withValue((String)obj));
                } else if (obj instanceof Integer) {
                    NumberAttribute numberAttribute=Attribute.customNumber(key);
                    builder.apply(numberAttribute.withValue((Integer)obj));
                } else if (obj instanceof Float) {
                    NumberAttribute numberAttribute=Attribute.customNumber(key);
                    builder.apply(numberAttribute.withValue((Float)obj));
                } else if (obj instanceof Double) {
                    NumberAttribute numberAttribute=Attribute.customNumber(key);
                    builder.apply(numberAttribute.withValue((Double)obj));
                }  else if (obj instanceof Boolean) {
                    BooleanAttribute booleanAttribute=Attribute.customBoolean(key);
                    builder.apply(booleanAttribute.withValue((Boolean)obj));
                } else {
                    StringAttribute stringAttribute= Attribute.customString(key);
                    builder.apply(stringAttribute.withValue(obj.toString()));
                }

            }

            if (name!=null) {
                NameAttribute nameAttribute=Attribute.name();
                builder.apply(nameAttribute.withValue(name));
            }

            if (gender!=Gender.NOT_KNOWN) {
                GenderAttribute genderAttribute=Attribute.gender();
                switch (gender) {
                    case MALE:
                        builder.apply(genderAttribute.withValue(GenderAttribute.Gender.MALE));
                        break;
                    case FEMALE:
                        builder.apply(genderAttribute.withValue(GenderAttribute.Gender.FEMALE));
                        break;
                    case OTHER:
                        builder.apply(genderAttribute.withValue(GenderAttribute.Gender.OTHER));
                        break;
                }
            }

            if (dob_calendar!=null) {
                BirthDateAttribute birthDateAttribute=Attribute.birthDate();
                builder.apply(birthDateAttribute.withBirthDate(dob_calendar));
            } else if ((dob_day!=NO_DOB_DAY) && (dob_month!=NO_DOB_DAY) && (dob_year!=NO_DOB_YEAR)) {
                BirthDateAttribute birthDateAttribute=Attribute.birthDate();
                builder.apply(birthDateAttribute.withBirthDate(dob_year,dob_month,dob_day));
            } else if ((dob_month!=NO_DOB_DAY) && (dob_year!=NO_DOB_YEAR)) {
                BirthDateAttribute birthDateAttribute=Attribute.birthDate();
                builder.apply(birthDateAttribute.withBirthDate(dob_year,dob_month));
            } else if (age!=NO_AGE) {
                BirthDateAttribute birthDateAttribute=Attribute.birthDate();
                builder.apply(birthDateAttribute.withAge(age));
            }

            YandexMetrica.reportUserProfile(builder.build());
        }


        //Convert attributes for Amplitude and Mixpanel

        if (name!=null) {
            userAttributes.put(NAME,name);
        }
        if (gender!=Gender.NOT_KNOWN) {
            userAttributes.put(GENDER,gender.name());
        }

        if (dob_calendar!=null) {
            userAttributes.put(DOB,dob_calendar.getTime().toGMTString());
        } else if ((dob_day!=NO_DOB_DAY) && (dob_month!=NO_DOB_DAY) && (dob_year!=NO_DOB_YEAR)) {
            userAttributes.put(DOB,dob_year+"/"+dob_month+"/"+dob_day);
        } else if ((dob_month!=NO_DOB_DAY) && (dob_year!=NO_DOB_YEAR)) {
            userAttributes.put(DOB,dob_year+"/"+dob_month);
        } else if (age!=NO_AGE) {
            userAttributes.put(AGE,age);
        }

        try {
            JSONObject props = new JSONObject();
            for (String key:userAttributes.keySet()) {
                props.put(key,userAttributes.get(key));
            }

            if (analytics_AmplitudeActive) {
                reportUserPropertiesAmplitude(props);
            }

        }  catch (JSONException e) {
            CustomLog.logException(e);
        }
        //TODO: also write to (encrypted) log file
    }

    /**
     * Report profile
     * Does not update existing values (for Yandex - withValueIfUndefined)
     * Following types supported:String,Integer,Float,Double,Boolean (and all others using 'toString')
     * @param userAttributes
     * @param name - value of special attribute 'name' (could be null)
     * @param gender  - value of special attribute 'gender' (use Gender.Gender.NOT_KNOWN if none known)
     * @param age - value of special attribute 'age' (use NO_AGE if none known)
     * @param dob_year - value of special attribute 'year of birth' (use NO_DOB_YEAR if none known)
     * @param dob_month - value of special attribute 'month of birth' (use NO_DOB_MONTH if none known)
     * @param dob_day - value of special attribute 'year of birth' (use NO_DOB_DAY if none known)
     * @param dob_calendar - value of special attribute 'calendar' (used as way to get date of birth) (use null if none known)
     */
    public static void reportProfileIfUndefined(Map<String, Object> userAttributes,String name,Gender gender,
                                                int age, int dob_year, int dob_month, int dob_day, Calendar dob_calendar ) {
        UserProfile.Builder builder=com.yandex.metrica.profile.UserProfile.newBuilder();

        if (analytics_YandexMetricaActive) {
            for (String key:userAttributes.keySet()) {
                Object obj=userAttributes.get(key);

                if (obj==null) {
                    StringAttribute stringAttribute= Attribute.customString(key);
                    builder.apply(stringAttribute.withValueIfUndefined("<null>"));
                } else if (obj instanceof String) {
                    StringAttribute stringAttribute= Attribute.customString(key);
                    builder.apply(stringAttribute.withValueIfUndefined((String)obj));
                } else if (obj instanceof Integer) {
                    NumberAttribute numberAttribute=Attribute.customNumber(key);
                    builder.apply(numberAttribute.withValueIfUndefined((Integer)obj));
                } else if (obj instanceof Float) {
                    NumberAttribute numberAttribute=Attribute.customNumber(key);
                    builder.apply(numberAttribute.withValueIfUndefined((Float)obj));
                } else if (obj instanceof Double) {
                    NumberAttribute numberAttribute=Attribute.customNumber(key);
                    builder.apply(numberAttribute.withValueIfUndefined((Double)obj));
                } else if (obj instanceof Boolean) {
                    BooleanAttribute booleanAttribute=Attribute.customBoolean(key);
                    builder.apply(booleanAttribute.withValueIfUndefined((Boolean)obj));
                }  else {
                    StringAttribute stringAttribute= Attribute.customString(key);
                    builder.apply(stringAttribute.withValueIfUndefined(obj.toString()));
                }

            }

            if (name!=null) {
                NameAttribute nameAttribute=Attribute.name();
                builder.apply(nameAttribute.withValueIfUndefined(name));
            }

            if (gender!=Gender.NOT_KNOWN) {
                GenderAttribute genderAttribute=Attribute.gender();
                switch (gender) {
                    case MALE:
                        builder.apply(genderAttribute.withValueIfUndefined(GenderAttribute.Gender.MALE));
                        break;
                    case FEMALE:
                        builder.apply(genderAttribute.withValueIfUndefined(GenderAttribute.Gender.FEMALE));
                        break;
                    case OTHER:
                        builder.apply(genderAttribute.withValueIfUndefined(GenderAttribute.Gender.OTHER));
                        break;
                }
            }


            if (dob_calendar!=null) {
                BirthDateAttribute birthDateAttribute=Attribute.birthDate();
                builder.apply(birthDateAttribute.withBirthDateIfUndefined(dob_calendar));
            } else if ((dob_day!=NO_DOB_DAY) && (dob_month!=NO_DOB_DAY) && (dob_year!=NO_DOB_YEAR)) {
                BirthDateAttribute birthDateAttribute=Attribute.birthDate();
                builder.apply(birthDateAttribute.withBirthDateIfUndefined(dob_year,dob_month,dob_day));
            } else if ((dob_month!=NO_DOB_DAY) && (dob_year!=NO_DOB_YEAR)) {
                BirthDateAttribute birthDateAttribute=Attribute.birthDate();
                builder.apply(birthDateAttribute.withBirthDateIfUndefined(dob_year,dob_month));
            } else if (age!=NO_AGE) {
                BirthDateAttribute birthDateAttribute=Attribute.birthDate();
                builder.apply(birthDateAttribute.withAgeIfUndefined(age));
            }
            YandexMetrica.reportUserProfile(builder.build());
        }

        //Convert attributes for Amplitude and Mixpanel


        if (name!=null) {
            userAttributes.put(NAME,name);
        }
        if (gender!=Gender.NOT_KNOWN) {
            userAttributes.put(GENDER,gender.name());
        }

        if (dob_calendar!=null) {
            userAttributes.put(DOB,dob_calendar.getTime().toGMTString());
        } else if ((dob_day!=NO_DOB_DAY) && (dob_month!=NO_DOB_DAY) && (dob_year!=NO_DOB_YEAR)) {
            userAttributes.put(DOB,dob_year+"/"+dob_month+"/"+dob_day);
        } else if ((dob_month!=NO_DOB_DAY) && (dob_year!=NO_DOB_YEAR)) {
            userAttributes.put(DOB,dob_year+"/"+dob_month);
        } else if (age!=NO_AGE) {
            userAttributes.put(AGE,age);
        }

        try {
            JSONObject props = new JSONObject();
            for (String key:userAttributes.keySet()) {
                props.put(key,userAttributes.get(key));
            }

            if (analytics_AmplitudeActive) {
                //TODO:honor ifNotExiist-style versions (requires changing wrapper)
                reportUserPropertiesAmplitude(props);
            }

        }  catch (JSONException e) {
            CustomLog.logException(e);
        }
        //TODO: also write to (encrypted) log file
    }
    /**
     *
     * @param identityAmplitude
     */
    public static void setUserIdentityAmplitude(Identify identityAmplitude) {
        if (analytics_AmplitudeActive) {
            try {
                Amplitude.getInstance().identify(identityAmplitude);
            } catch (Exception e) {
                CustomLog.logException(e);
            }
        }

    }

    /**
     * Sets profile id for Yandex.Metrica AND Amplitude (if any enabled)
     * @param profileIdForAnalytics
     */
    public static void setProfileIdForAnalytics(String profileIdForAnalytics) {
        setUserIdAmplitude(profileIdForAnalytics);
        setUserIDYandexMetrica(profileIdForAnalytics);
    }
    /**
     * Sets YandexMetrica profile id
     * @param userIDYandexMetrica
     */
    public static void setUserIDYandexMetrica(String userIDYandexMetrica) {
        if (analytics_YandexMetricaActive) {
            try {
                YandexMetrica.setUserProfileID(userIDYandexMetrica);
            } catch (Exception e) {
                CustomLog.logException(e);
            }
        }

    }
    /**
     * Sets Amplitude user Id
     * @param userIdAmplitude
     */
    public static void setUserIdAmplitude(String userIdAmplitude) {
        if (analytics_AmplitudeActive) {
            try {
                Amplitude.getInstance().setUserId(userIdAmplitude);
            } catch (Exception e) {
                CustomLog.logException(e);
            }
        }

    }

    /**
     * Report custom user properties to Amplitude
     * <code>
     *     JSONObject songProperties = new JSONObject();
     *      try {
     *            eventProperties.put("title", "Here comes the Sun");
     *            eventProperties.put("artist", "The Beatles");
     *            eventProperties.put("genre", "Rock");
     *          } catch (JSONException exception) {
     *       }
     * </code>
     * @param properties
     */
    public static void reportUserPropertiesAmplitude(JSONObject properties) {
        if (analytics_AmplitudeActive) {
            try {
                Amplitude.getInstance().setUserProperties(properties);
            } catch (Exception e) {
                CustomLog.logException(e);
            }
        }
    }

    /**
     * Reports revenue to Amplitude
     * @param revenue
     */
    public static  void reportRevenueAmplitude(com.amplitude.api.Revenue revenue) {
        if (analytics_AmplitudeActive) {
            try {
                Amplitude.getInstance().logRevenueV2(revenue);
            } catch (Exception e) {
                CustomLog.logException(e);
            }
        }
    }

    /**
     * Reports revenu to Yandex
     * @param yandexRev
     */
    public static void reportRevenuYandex(Revenue yandexRev) {
        if (analytics_YandexMetricaActive) {
            try {
                YandexMetrica.reportRevenue(yandexRev);

            } catch (Exception e) {
                CustomLog.logException(e);
            }
        }
    }
    /**
     * Helper to safely work with progress dialogs, accounting for possible issues like dialog being in another view hierarchy
     * based off http://stackoverflow.com/questions/2224676/android-view-not-attached-to-window-manager
     * @param context
     * @param dialog
     */
    public static boolean isDialogUsable(Context context, AlertDialog dialog) {
        //Can't touch other View of other Activiy..
        //http://stackoverflow.com/questions/23458162/dismiss-progress-dialog-in-another-activity-android
        if (context==null) {
            CustomLog.e(TAG,"Null context passed");
            CustomLog.logException(new RuntimeException("Null context passed"));
            return false;
        }
        if ( (dialog != null) && dialog.isShowing()) {

            //is it the same context from the caller ?
            CustomLog.w(TAG, "the dialog is from "+dialog.getContext());

            Class caller_context= context.getClass();
            Activity call_Act = (Activity)context;
            Class progress_context= dialog.getContext().getClass();

            Boolean is_act= ( (dialog.getContext()) instanceof  Activity )?true:false;
            Boolean is_ctw= ( (dialog.getContext()) instanceof ContextThemeWrapper)?true:false;

            if (is_ctw) {
                ContextThemeWrapper cthw=(ContextThemeWrapper) dialog.getContext();
                Boolean is_same_acivity_with_Caller= ((Activity)(cthw).getBaseContext() ==  call_Act )?true:false;

                if (is_same_acivity_with_Caller){
                    //ok to work with dialog
                    return true;
                }
                else {
                    CustomLog.e(TAG, "the dialog is NOT from the same context! Can't touch.."+((Activity)(cthw).getBaseContext()).getClass());
                    //not ok to work with dialog
                    return false;
                }
            } else {
                CustomLog.e(TAG,progress_context.getName()+" is not ctw(1)");
                //not ok to work with dialog - not ContextThemeWrapper
                return false;
            }
        } else {
            //dialog is either null or not showing
            return false;
        }

    }
    /**
     * Helper to safely dismiss progress dialog, accounting for possible issues like dialog being in another view hierarchy
     * based off http://stackoverflow.com/questions/2224676/android-view-not-attached-to-window-manager
     * @param context
     * @param dialog
     */
    public static void dismissProgressDialog(Context context, ProgressDialog dialog) {
        //Can't touch other View of other Activiy..
        //http://stackoverflow.com/questions/23458162/dismiss-progress-dialog-in-another-activity-android
        if (context==null) {
            CustomLog.e(TAG,"Null context passed");
            CustomLog.logException(new RuntimeException("Null context passed"));
            return;
        }
        if ( (dialog != null) && dialog.isShowing()) {

            //is it the same context from the caller ?
            CustomLog.w(TAG, "the dialog is from "+dialog.getContext());

            Class caller_context= context.getClass();
            Activity call_Act = (Activity)context;
            Class progress_context= dialog.getContext().getClass();

            Boolean is_act= ( (dialog.getContext()) instanceof  Activity )?true:false;
            Boolean is_ctw= ( (dialog.getContext()) instanceof ContextThemeWrapper)?true:false;

            if (is_ctw) {
                ContextThemeWrapper cthw=(ContextThemeWrapper) dialog.getContext();
                Boolean is_same_acivity_with_Caller= ((Activity)(cthw).getBaseContext() ==  call_Act )?true:false;

                if (is_same_acivity_with_Caller){
                    dialog.dismiss();
                    dialog = null;
                }
                else {
                    CustomLog.e(TAG, "the dialog is NOT from the same context! Can't touch.."+((Activity)(cthw).getBaseContext()).getClass());
                    dialog = null;
                }
            } else {
                CustomLog.e(TAG,progress_context.getName()+" is not ctw(1)");
            }


        }
    }
    /**
     * Helper to safely cancel alert dialog, accounting for possible issues like dialog being in another view hierarchy
     * based off http://stackoverflow.com/questions/2224676/android-view-not-attached-to-window-manager
     * @param context
     * @param dialog
     */
    public static void cancelAlertDialogFromSupportLibrary(Context context, android.support.v7.app.AlertDialog dialog) {
        //Can't touch other View of other Activiy..
        //http://stackoverflow.com/questions/23458162/dismiss-progress-dialog-in-another-activity-android
        if (context==null) {
            CustomLog.e(TAG,"Null context passed");
            CustomLog.logException(new RuntimeException("Null context passed"));
            return;
        }
        if ( (dialog != null) && dialog.isShowing()) {

            //is it the same context from the caller ?
            CustomLog.w(TAG, "the dialog is from "+dialog.getContext());

            Class caller_context= context.getClass();
            Activity call_Act = (Activity)context;
            Class progress_context= dialog.getContext().getClass();

            Boolean is_act= ( (dialog.getContext()) instanceof  Activity )?true:false;
            Boolean is_ctw= ( (dialog.getContext()) instanceof ContextThemeWrapper)?true:false;

            if (is_ctw) {
                ContextThemeWrapper cthw=(ContextThemeWrapper) dialog.getContext();
                Boolean is_same_acivity_with_Caller= ((Activity)(cthw).getBaseContext() ==  call_Act )?true:false;

                if (is_same_acivity_with_Caller){
                    dialog.cancel();
                    dialog = null;
                }
                else {
                    CustomLog.e(TAG, "the dialog is NOT from the same context! Can't touch.."+((Activity)(cthw).getBaseContext()).getClass());
                    dialog = null;
                }
            } else {
                CustomLog.e(TAG,progress_context.getName()+" is not ctw(1)");
            }


        }
    }
    /**
     * Helper to safely dismiss alert dialog, accounting for possible issues like dialog being in another view hierarchy
     * based off http://stackoverflow.com/questions/2224676/android-view-not-attached-to-window-manager
     * @param context
     * @param dialog
     */
    public static void dismissAlertDialogFromSupportLibrary(Context context, android.support.v7.app.AlertDialog dialog) {
        //Can't touch other View of other Activiy..
        //http://stackoverflow.com/questions/23458162/dismiss-progress-dialog-in-another-activity-android
        if (context==null) {
            CustomLog.e(TAG,"Null context passed");
            CustomLog.logException(new RuntimeException("Null context passed"));
            return;
        }
        if ( (dialog != null) && dialog.isShowing()) {

            //is it the same context from the caller ?
            CustomLog.w(TAG, "the dialog is from "+dialog.getContext());

            Class caller_context= context.getClass();
            Activity call_Act = (Activity)context;
            Class progress_context= dialog.getContext().getClass();

            Boolean is_act= ( (dialog.getContext()) instanceof  Activity )?true:false;
            Boolean is_ctw= ( (dialog.getContext()) instanceof ContextThemeWrapper)?true:false;

            if (is_ctw) {
                ContextThemeWrapper cthw=(ContextThemeWrapper) dialog.getContext();
                Boolean is_same_acivity_with_Caller= ((Activity)(cthw).getBaseContext() ==  call_Act )?true:false;

                if (is_same_acivity_with_Caller){
                    dialog.dismiss();
                    dialog = null;
                }
                else {
                    CustomLog.e(TAG, "the dialog is NOT from the same context! Can't touch.."+((Activity)(cthw).getBaseContext()).getClass());
                    dialog = null;
                }
            } else {
                CustomLog.e(TAG,progress_context.getName()+" is not ctw(1)");
            }


        }
    }

    /**
     * Returns a user agent string based on the given application name
     *
     * @param context A valid context of the calling application.
     * @param applicationName String that will be prefix'ed to the generated user agent.
     * @return A user agent string generated using the applicationName.
     */
    public static String getUserAgent(Context context, String applicationName) {
        String versionName;
        try {
            String packageName = context.getPackageName();
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            versionName = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            versionName = "?";
        }
        return applicationName + "/" + versionName + " (Linux;Android " + Build.VERSION.RELEASE
                + ") ";
    }

    /**
     * method checks to see if app is currently set as default launcher
     * @return boolean true means currently set as default, otherwise false
     */
    public static boolean isMyAppLauncherDefault(Context context) {
        final IntentFilter filter = new IntentFilter(Intent.ACTION_MAIN);
        filter.addCategory(Intent.CATEGORY_HOME);

        List<IntentFilter> filters = new ArrayList<IntentFilter>();
        filters.add(filter);
        final String myPackageName = context.getPackageName();
        List<ComponentName> activities = new ArrayList<ComponentName>();
        final PackageManager packageManager = (PackageManager) context.getPackageManager();

        packageManager.getPreferredActivities(filters, activities, null);

        for (ComponentName activity : activities) {
            if (myPackageName.equals(activity.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Utility method for debugging OpenGL calls. Provide the name of the call
     * just after making it:
     *
     * <pre>
     * mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
     * MyGLRenderer.checkGlError("glGetUniformLocation");</pre>
     *
     * If the operation is not successful, the check throws an error.
     *
     * @param glOperation - Name of the OpenGL call to check.
     */
    public static void checkGlError(String tag, String glOperation) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            CustomLog.e(tag, glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }


    /**
     * Расстояние в метрах из GPS-координат
     * @param location1 начальная координата
     * @param location2 конечная координата
     * @return Расстояние в метрах между точками
     */
    public static float distFrom(Location location1,Location location2) {
        return distFrom(
                location1.getLatitude(),location1.getLongitude(),
                location2.getLatitude(),location2.getLongitude());
    }
    /**
     * Расстояние в метрах из GPS-координат
     * https://stackoverflow.com/questions/837872/calculate-distance-in-meters-when-you-know-longitude-and-latitude-in-java
     * @param lat1 начальная широта
     * @param lng1 начальная долгота
     * @param lat2 конечная широта
     * @param lng2 конечная долгота
     * @return Расстояние в метрах между точками
     */
    public static float distFrom(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000; //радиус Земли в метрах
        double dLat = Math.toRadians(lat2-lat1);
        double dLng = Math.toRadians(lng2-lng1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng/2) * Math.sin(dLng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        float dist = (float) (earthRadius * c);

        return dist;
    }

    /**
     * Преобразование из GPS-координат на плоскость
     * на основе https://stackoverflow.com/questions/3024404/transform-longitude-latitude-into-meters?rq=1
     * Мирные советские тракторы поддерживаются только в наземном режиме.
     * @param baseLocation координаты опорной точки
     * @param location текущие координаты
     * @return
     */
    public static Geometry.Point getToXY(Location baseLocation, Location location) {

        double deltaLatitude = location.getLatitude() - baseLocation.getLatitude();
        double deltaLongitude = location.getLongitude() - baseLocation.getLongitude();
        /*
            The circumference at the equator (latitude 0) is 40075160 meters.
            The circumference of a circle at a given latitude will be proportional to the cosine, so
            the formula will be deltaLongitude * 40075160 * cos(latitude) / 360
         */
        double latitudeCircumference = 40075160.0 * Math.cos(Math.toRadians(baseLocation.getLatitude()));
        double resultX = deltaLongitude * latitudeCircumference / 360.0;

        /*
            We know that 360 degrees is a full circle around the earth through the poles,
            and that distance is 40008000 meters. As long as you don't need to account for
            the errors due to the earth being not perfectly spherical,
            the formula is deltaLatitude * 40008000 / 360.
         */
        double resultY = deltaLatitude * 40008000.0 / 360.0;
        //Point тут используется не совсем по назначению, нужен класс вроде 2D location...
        //потому что будет еще фактор масштабирования использоваться
        Geometry.Point result=new Geometry.Point((float)resultX,(float)resultY,0f);
        return result;
    }

    /**
     * Based off http://stackoverflow.com/questions/2785485/is-there-a-unique-android-device-id
     * not use play services ad id
     * Return pseudo unique ID
     * Will never return null. Could get collisions on API<9
     * @return pseudo unique ID for this device
     */
    public static String getUniquePsuedoID() {
        // If all else fails, if the user does have lower than API 9 (lower
        // than Gingerbread), has reset their device or 'Secure.ANDROID_ID'
        // returns 'null', then simply the ID returned will be solely based
        // off their Android device information. This is where the collisions
        // can happen.
        // Thanks http://www.pocketmagic.net/?p=1662!
        // Try not to use DISPLAY, HOST or ID - these items could change.
        // If there are collisions, there will be overlapping data
        String m_szDevIDShort = "35" + (Build.BOARD.length() % 10) + (Build.BRAND.length() % 10) + (Build.CPU_ABI.length() % 10) + (Build.DEVICE.length() % 10) + (Build.MANUFACTURER.length() % 10) + (Build.MODEL.length() % 10) + (Build.PRODUCT.length() % 10);

        // Thanks to @Roman SL!
        // http://stackoverflow.com/a/4789483/950427
        // Only devices with API >= 9 have android.os.Build.SERIAL
        // http://developer.android.com/reference/android/os/Build.html#SERIAL
        // If a user upgrades software or roots their device, there will be a duplicate entry
        String serial = null;
        try {
            serial = android.os.Build.class.getField("SERIAL").get(null).toString();

            // Go ahead and return the serial for api => 9
            return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
        } catch (Exception exception) {
            // String needs to be initialized
            serial = "serial"; // some value
        }

        // Thanks @Joe!
        // http://stackoverflow.com/a/2853253/950427
        // Finally, combine the values we have found by using the UUID class to create a unique identifier
        return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
    }

    //-----------------------------------------------------------------------

    /**
     * <p>Gets the stack trace from a Throwable as a String.</p>
     * <p>
     * <p>The result of this method vary by JDK version as this method
     * uses {@link Throwable#printStackTrace(java.io.PrintWriter)}.
     * On JDK1.3 and earlier, the cause exception will not be shown
     * unless the specified throwable alters printStackTrace.</p>
     *
     * @param throwable the <code>Throwable</code> to be examined
     * @return the stack trace as generated by the exception's
     * <code>printStackTrace(PrintWriter)</code> method
     * <p>
     * Credit: https://commons.apache.org/proper/commons-lang/apidocs/src-html/org/apache/commons/lang3/exception/ExceptionUtils.html
     * See also joinStackTrace in CrashHandlerUtil - it handles 'caused by' case
     */
    public static String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    // Credit: http://stackoverflow.com/questions/2799097/how-can-i-detect-when-an-android-application-is-running-in-the-emulator
    public static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }

    public static boolean isEmptyString(@Nullable String str) {
        return str == null || str.length() == 0;
    }

}
