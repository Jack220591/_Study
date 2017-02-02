package com.erik.clips.fragments;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NoConnectionError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.lsjwzh.widget.materialloadingprogressbar.CircleProgressBar;
import com.marshalchen.ultimaterecyclerview.ItemTouchListenerAdapter;
import com.marshalchen.ultimaterecyclerview.UltimateRecyclerView;
import com.erik.clips.R;
import com.erik.clips.adapters.AdapterList;
import com.erik.clips.utils.MySingleton;
import com.erik.clips.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Design and developed by pongodev.com
 *
 * FragmentNewVideos is created to display the latest video data of a youtube channel or playlist.
 * Created using Fragment.
 * FragmentNewVideos создан для отображения последних видео данных канала YouTube или список воспроизведения.
 * Созданный с использованием фрагмента.
 */
public class FragmentNewVideos extends Fragment implements View.OnClickListener {

    // Create tag for log
    // Создаем ТЭГ для Логов
    private static final String TAG = FragmentNewVideos.class.getSimpleName();
    // Create view objects
    // создание view объектов
    private TextView mLblNoResult;
    private LinearLayout mLytRetry;
    private CircleProgressBar mPrgLoading;
    private UltimateRecyclerView mUltimateRecyclerView;
    private AdView mAdView;

    // Create variable to handle admob visibility
    // Создать переменную для обработки AdMob visibility
    private boolean mIsAdmobVisible;

    // Create listener
    // Создание слушателя
    private OnVideoSelectedListener mCallback;

    // Create AdapterList object
    // Создание объекта AdapterList
    private AdapterList mAdapterList = null;

    // Create arraylist variable to store video data before get video duration
    // Создать Arraylist переменную для хранения видеоданных до получения длительности  видео
    private ArrayList<HashMap<String, String>> mTempVideoData = new ArrayList<>();
    // Create arraylist variable to store final data
    // Создать Arraylist переменную для хранения конечных данных
    private ArrayList<HashMap<String, String>> mVideoData     = new ArrayList<>();

    private String mNextPageToken = "";
    private int mNumberOfLooping = 0;
    private String mVideoIds = "";
    private String mDuration = "00:00";

    // Paramater (true = is first time, false = not first time)
    // Параметр (истина = это первый раз, ложь = не первый раз)
    private boolean mIsAppFirstLaunched = true;

    private ArrayList<String> mChannelNames, mVideoTypes, mChannelIds;

    // Create variable to check the first video
    // Создать переменную, для проверки первого видео
    private boolean mIsFirstVideo = true;

    // Interface, activity that use FragmentRecipes must implement onRecipeSelecte method
    // Интерфейс, который использует FragmentRecipes и должен реализовывать метод onRecipeSelecte
    public interface OnVideoSelectedListener {
        public void onVideoSelected(String ID);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        View view = inflater.inflate(R.layout.fragment_video_list, container, false);

        // Get Bundle data
        // Получить Bundle данные
        Bundle bundle = this.getArguments();
        setHasOptionsMenu(true);
        // Get data from ActivityHome
        // Получение данных из ActivityHome
        mChannelNames = new ArrayList<>(Arrays.asList(bundle.getStringArray(Utils.TAG_CHANNEL_NAMES)));
        mVideoTypes = new ArrayList<>(Arrays.asList(bundle.getStringArray(Utils.TAG_VIDEO_TYPE)));
        mChannelIds = new ArrayList<>(Arrays.asList(bundle.getStringArray(Utils.TAG_CHANNEL_IDS)));

        // Remove the first data as it is "New Videos"
        // Удаление первых данных  какбуд-то это новое видео
        mChannelNames.remove(0);
        mVideoTypes.remove(0);
        mChannelIds.remove(0);

        // Connect view objects and view ids from xml
        // Соединяем view objects и ID из xml
        mUltimateRecyclerView = (UltimateRecyclerView) view.findViewById(R.id.ultimate_recycler_view);
        mLblNoResult          = (TextView) view.findViewById(R.id.lblNoResult);
        mLytRetry             = (LinearLayout) view.findViewById(R.id.lytRetry);
        mPrgLoading           = (CircleProgressBar) view.findViewById(R.id.prgLoading);
        Button btnRetry       = (Button) view.findViewById(R.id.raisedRetry);
        mAdView               = (AdView) view.findViewById(R.id.adView);

        // Set click listener to btnRetry
        // Назначаем слушателя для btnRetry
        btnRetry.setOnClickListener(this);
        // Set circular bar color and visibility
        // Устанавливаем цвет для circular bar и делаем его видимым
        mPrgLoading.setColorSchemeResources(R.color.accent_color);
        mPrgLoading.setVisibility(View.VISIBLE);

        // Set value to true in default
        // Устанавливаем значение по умолчанию - true
        mIsAppFirstLaunched = true;

        // Get admob visibility value
        // Получаем значение AdMob visibility
        mIsAdmobVisible = Utils.admobVisibility(mAdView, Utils.IS_ADMOB_VISIBLE);
        // Load ad in background using asynctask class
        // Загрузка объявления в фоновом режиме, используя класс AsyncTask
        new SyncShowAd(mAdView).execute();

        // Set arraylist variable of videoData
        mVideoData = new ArrayList<>();

        // Set mAdapterList to UltimateRecyclerView object
        // Заполняем объект UltimateRecyclerView из mAdapterList
        mAdapterList = new AdapterList(getActivity(), mVideoData);
        mUltimateRecyclerView.setAdapter(mAdapterList);
        mUltimateRecyclerView.setHasFixedSize(false);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        mUltimateRecyclerView.setLayoutManager(linearLayoutManager);

        // Condition when item in list is click
        // Сосотояние когда элемент из списка выбран
        ItemTouchListenerAdapter itemTouchListenerAdapter =
                new ItemTouchListenerAdapter(mUltimateRecyclerView.mRecyclerView,
                new ItemTouchListenerAdapter.RecyclerViewOnItemClickListener() {
                    @Override
                    public void onItemClick(RecyclerView parent, View clickedView, int position) {
                        // To handle when position  = locationsData.size means loading view is click
                        // ???
                        if (position < mVideoData.size()) {
                            // Pass data to onVideoSelected in ActivityHome
                            // Передавать данные onVideoSelected в ActivityHome
                            mCallback.onVideoSelected(mVideoData.get(position)
                                    .get(Utils.KEY_VIDEO_ID));
                        }
                    }

                    @Override
                    public void onItemLongClick(RecyclerView recyclerView, View view, int i) {
                    }
                });

        // Enable touch listener
        // Включить сенсор слушателя
        mUltimateRecyclerView.mRecyclerView.addOnItemTouchListener(itemTouchListenerAdapter);

        // Get data from server in first time when fragment create
        // Получить данные с сервера в первый раз, при создание объекта
        getVideoData();

        return view;
    }
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception.
        // Это гарантирует, что была осуществлена деятельность
        //  интерфейса обратного вызова. Если нет, то он вызывает исключение.
        try {
            mCallback = (OnVideoSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnVideoSelectedListener");
        }
    }


    // Asynctask class to load admob in background
    // AsyncTask класс для загрузки AdMob в фоновом режиме
    public class SyncShowAd extends AsyncTask<Void, Void, Void>{

        AdView ad;
        AdRequest adRequest, interstitialAdRequest;
        InterstitialAd interstitialAd;
        int interstitialTrigger;

        public SyncShowAd(AdView ad){
            this.ad = ad;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            // Check ad visibility. If visible, create adRequest
            // Проверка видимости объявлений. Если видно, создать AdRequest
            if(mIsAdmobVisible) {
                // Create an ad request
                // Создать запрос объявления
                if (Utils.IS_ADMOB_IN_DEBUG) {
                    adRequest = new AdRequest.Builder().
                            addTestDevice(AdRequest.DEVICE_ID_EMULATOR).build();
                } else {
                    adRequest = new AdRequest.Builder().build();
                }

                // When interstitialTrigger equals ARG_TRIGGER_VALUE, display interstitial ad
                // Когда interstitialTrigger равна ARG_TRIGGER_VALUE, отображать интерстициальне объявление
                interstitialAd = new InterstitialAd(getActivity());
                interstitialAd.setAdUnitId(getActivity().getResources()
                        .getString(R.string.interstitial_ad_unit_id));
                interstitialTrigger = Utils.loadIntPreferences(getActivity(),
                        Utils.ARG_ADMOB_PREFERENCE,
                        Utils.ARG_TRIGGER);
                Log.d("Interstitial", "trigger: "+interstitialTrigger);
                if(interstitialTrigger == Utils.ARG_TRIGGER_VALUE) {
                    if(Utils.IS_ADMOB_IN_DEBUG) {
                        Log.d("Interstitial", "debugging");
                        interstitialAdRequest = new AdRequest.Builder()
                                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                                .build();
                    }else {
                        Log.d("Interstitial", "release");
                        interstitialAdRequest = new AdRequest.Builder().build();
                    }
                    Utils.saveIntPreferences(getActivity(),
                            Utils.ARG_ADMOB_PREFERENCE,
                            Utils.ARG_TRIGGER,
                            1);
                }else{
                    Utils.saveIntPreferences(getActivity(),
                            Utils.ARG_ADMOB_PREFERENCE,
                            Utils.ARG_TRIGGER,
                            (interstitialTrigger+1));
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            // Check ad visibility. If visible, display ad banner and interstitial
            // Проверка видимости объявлений. Если видно, дисплей рекламный баннер и interstitial
            if(mIsAdmobVisible) {
                // Start loading the ad
                // Начать загрузку объявления
                ad.loadAd(adRequest);

                if (interstitialTrigger == Utils.ARG_TRIGGER_VALUE) {
                    // Start loading the ad
                    interstitialAd.loadAd(interstitialAdRequest);

                    // Set the AdListener
                    // Назначаем слушателя
                    interstitialAd.setAdListener(new AdListener() {
                        @Override
                        public void onAdLoaded() {
                            if (interstitialAd.isLoaded()) {
                                interstitialAd.show();
                            }
                        }

                        @Override
                        public void onAdFailedToLoad(int errorCode) {

                        }

                        @Override
                        public void onAdClosed() {

                        }

                    });
                }
            }

        }
    }

    // Method to get videos data of a youtube channel of playlist
    // Метод, чтобы получить видео данные канала YouTube из списка воспроизведения
    private void getVideoData() {
        for(int j = 0; j < mChannelIds.size(); j++) {
            // Create array variable to store first video id of the videos channel
            // Создать переменную массива для хранения первого идентификатора видеоканала
            final String[] videoId = new String[1];

            // Create variable to store youtube api url
            // Создаем переменную для хранения YouTube API Url
            String url;
            // Get video type
            // олучить тип видео
            final int mVideoType = Integer.parseInt(mVideoTypes.get(j));
            // Check whether it is channel or playlist
            //Проверка, является ли это каналом или плейлистом
            if(mVideoType == 2) {
                // Youtube API url for playlist
                // Youtube API url для плэйлиста
                url = Utils.API_YOUTUBE + Utils.FUNCTION_PLAYLIST_ITEMS_YOUTUBE +
                        Utils.PARAM_PART_YOUTUBE + "snippet,id&" +
                        Utils.PARAM_FIELD_PLAYLIST_YOUTUBE + "&" +
                        Utils.PARAM_KEY_YOUTUBE + getResources().getString(R.string.youtube_apikey) + "&" +
                        Utils.PARAM_PLAYLIST_ID_YOUTUBE + mChannelIds.get(j) + "&" +
                        Utils.PARAM_PAGE_TOKEN_YOUTUBE + mNextPageToken + "&" +
                        Utils.PARAM_MAX_RESULT_YOUTUBE + Utils.ARG_NUMBER_OF_NEW_VIDEO;

            }else {
                // Youtube API url for channel
                // Youtube API url для канала
                url = Utils.API_YOUTUBE + Utils.FUNCTION_SEARCH_YOUTUBE +
                        Utils.PARAM_PART_YOUTUBE + "snippet,id&" + Utils.PARAM_ORDER_YOUTUBE + "&" +
                        Utils.PARAM_TYPE_YOUTUBE + "&" +
                        Utils.PARAM_FIELD_SEARCH_YOUTUBE + "&" +
                        Utils.PARAM_KEY_YOUTUBE + getResources().getString(R.string.youtube_apikey) + "&" +
                        Utils.PARAM_CHANNEL_ID_YOUTUBE + mChannelIds.get(j) + "&" +
                        Utils.PARAM_PAGE_TOKEN_YOUTUBE + mNextPageToken + "&" +
                        Utils.PARAM_MAX_RESULT_YOUTUBE + Utils.ARG_NUMBER_OF_NEW_VIDEO;
            }

            // Get youtube channel or playlist name
            // Получаем YouTube канал или плейлист
            final String channelName = mChannelNames.get(j);
            JsonObjectRequest request = new JsonObjectRequest(url, null,
                new Response.Listener<JSONObject>() {
                    JSONArray dataItemArray;
                    JSONObject itemIdObject, itemSnippetObject, itemSnippetThumbnailsObject,
                            itemSnippetResourceIdObject;

                    @Override
                    public void onResponse(JSONObject response) {
                        // Number of looping
                        // Номер зацикливания
                        mNumberOfLooping += 1;

                        // To make sure Activity is still in the foreground
                        // Чтобы убедиться, что активность по-прежнему на переднем плане
                        Activity activity = getActivity();
                        if (activity != null && isAdded()) {
                            try {
                                // Get all Items json Array from server
                                // Получить все элементы массива JSon с сервера
                                dataItemArray = response.getJSONArray(Utils.ARRAY_ITEMS);

                                if (dataItemArray.length() > 0) {
                                    haveResultView();
                                    for (int i = 0; i < dataItemArray.length(); i++) {
                                        HashMap<String, String> dataMap = new HashMap<>();
                                        // Detail Array per Item
                                        // ???
                                        JSONObject itemsObject = dataItemArray.getJSONObject(i);
                                        // Array snippet to get title and thumbnails
                                        // Массив сннипетов, чтобы получить заголовок и превью
                                        itemSnippetObject = itemsObject.
                                                getJSONObject(Utils.OBJECT_ITEMS_SNIPPET);

                                        if(mVideoType == 2){
                                            // Get video ID in playlist
                                            // Получаем ID видео в плейлисте
                                            itemSnippetResourceIdObject = itemSnippetObject.
                                                    getJSONObject(
                                                            Utils.OBJECT_ITEMS_SNIPPET_RESOURCEID);
                                            dataMap.put(Utils.KEY_VIDEO_ID,
                                                    itemSnippetResourceIdObject.getString(
                                                            Utils.KEY_VIDEO_ID));
                                            videoId[0] = itemSnippetResourceIdObject.
                                                    getString(Utils.KEY_VIDEO_ID);

                                            // Concat all video IDs and use it as parameter to
                                            // get all video durations.
                                            // Связываем все параметры видео по ID через запятую.
                                            mVideoIds = mVideoIds + itemSnippetResourceIdObject.
                                                    getString(Utils.KEY_VIDEO_ID) + ",";
                                        }else {
                                            // Get video ID in channel
                                            // олучаем ID видео в канале
                                            itemIdObject = itemsObject.
                                                    getJSONObject(Utils.OBJECT_ITEMS_ID);
                                            dataMap.put(Utils.KEY_VIDEO_ID,
                                                    itemIdObject.getString(Utils.KEY_VIDEO_ID));
                                            videoId[0] = itemIdObject.
                                                    getString(Utils.KEY_VIDEO_ID);
                                            // Concat all video IDs and use it as parameter to
                                            // get all video durations.
                                            // Связываем все параметры видео по ID через запятую.
                                            mVideoIds = mVideoIds + itemIdObject.
                                                    getString(Utils.KEY_VIDEO_ID) + ",";

                                        }

                                        // When fragment first created display first video to
                                        // video player.
                                        // Когда фрагмент впервые создан дисплей первого видео в
                                        // видеопроигрыватель.
                                        if(mIsFirstVideo && i == 0) {
                                            mIsFirstVideo = false;
                                            mCallback.onVideoSelected(videoId[0]);
                                        }

                                        // Get video title
                                        // олучаем заголовок видео
                                        dataMap.put(Utils.KEY_TITLE,
                                                itemSnippetObject.getString(Utils.KEY_TITLE));

                                        // Get published date
                                        // Получаем дату публикации
                                        dataMap.put(Utils.KEY_PUBLISHEDAT, channelName);

                                        // Get video thumbnail
                                        // Получаем превью видеозаписи
                                        itemSnippetThumbnailsObject = itemSnippetObject.
                                                getJSONObject(Utils.OBJECT_ITEMS_SNIPPET_THUMBNAILS);
                                        itemSnippetThumbnailsObject = itemSnippetThumbnailsObject.
                                                getJSONObject(
                                                        Utils.OBJECT_ITEMS_SNIPPET_THUMBNAILS_MEDIUM);
                                        dataMap.put(Utils.KEY_URL_THUMBNAILS,
                                                itemSnippetThumbnailsObject.getString(
                                                        Utils.KEY_URL_THUMBNAILS));

                                        // Store video data temporarily to get video duration
                                        // Временное хранилище для данных, чтобы получить продолжительность видеозаписи
                                        mTempVideoData.add(dataMap);
                                    }

                                    // When getting all videos of each channel finish,
                                    // get video duration.
                                    // При получении всех видео,
                                    //  получить продолжительность видео.
                                    if(mNumberOfLooping == mChannelIds.size()){
                                        getDuration();
                                    }

                                    // Condition if dataItemArray == result perpage it means maybe
                                    // server still have data
                                    // Если условие dataItemArray == результату per page
                                    // это означает что в канале остались еще видеозаписи
                                    if (dataItemArray.length() == Utils.PARAM_RESULT_PER_PAGE) {
                                        // To get next page data youtube have
                                        // parameter Next Page Token
                                        // Чтобы получить следующую страницу с данными на YouTube
                                        mNextPageToken = response.getString(Utils.ARRAY_PAGE_TOKEN);

                                        // No data anymore in this URL
                                        // Нет данных больше в этом URL
                                    } else {
                                        // Clear mNextPageToken
                                        mNextPageToken = "";
                                    }

                                    // If success get data, it means next it is not first time again.
                                    // Если данные получены успешно, ???
                                    mIsAppFirstLaunched = false;

                                    // Data from server already load all or no data in server
                                    // Данные с сервера уже загружены или их нет
                                } else {
                                    if (mIsAppFirstLaunched &&
                                            mAdapterList.getAdapterItemCount() <= 0) {
                                        noResultView();
                                    }
                                }

                            } catch (JSONException e) {
                                Log.d(Utils.TAG_PONGODEV + TAG,
                                        "JSON Parsing error: " + e.getMessage());
                                mPrgLoading.setVisibility(View.GONE);
                            }
                            mPrgLoading.setVisibility(View.GONE);
                        }
                    }
                },

                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // To make sure Activity is still in the foreground
                        // убедиться, что активность по-прежнему на переднем плане
                        Activity activity = getActivity();
                        if (activity != null && isAdded()) {
                            Log.d(Utils.TAG_PONGODEV + TAG,
                                    "on Error Response get video data: " + error.getMessage());
                            // "try-catch" To handle when still in process and
                            // then application closed.
                            try {
                                String msgSnackBar;
                                if (error instanceof NoConnectionError) {
                                    msgSnackBar = getResources().
                                            getString(R.string.no_internet_connection);
                                } else {
                                    msgSnackBar = getResources().
                                            getString(R.string.response_error);
                                }

                                // To handle when no data in mAdapter and then get
                                // error because no connection or problem in server
                                // Проверка когда нет данных в Адаптере или проблемы на сервере
                                if (mVideoData.size() == 0) {
                                    retryView();

                                    // Condition when loadmore it has data,
                                    // when loadmore then get error because no connection.
                                } else {
                                    mAdapterList.setCustomLoadMoreView(null);
                                    mAdapterList.notifyDataSetChanged();
                                }

                                //Utils.showSnackBar(getActivity(), msgSnackBar);
                                mPrgLoading.setVisibility(View.GONE);

                            } catch (Exception e) {
                                Log.d(Utils.TAG_PONGODEV + TAG,
                                        "failed catch volley " + e.toString());
                                mPrgLoading.setVisibility(View.GONE);
                            }
                        }
                    }
                }
            );
            request.setRetryPolicy(new DefaultRetryPolicy(Utils.ARG_TIMEOUT_MS,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            MySingleton.getInstance(getActivity()

            ).getRequestQueue().add(request);
        }

    }

    // Method to get video duration
    // Метод для получения продожительности видео
    private void getDuration() {
        // Youtube API url to get video duration
        // Youtube API url для получения продолжительности видео
        String url = Utils.API_YOUTUBE + Utils.FUNCTION_VIDEO_YOUTUBE +
            Utils.PARAM_PART_YOUTUBE + "contentDetails&" +
            Utils.PARAM_FIELD_VIDEO_YOUTUBE + "&" +
            Utils.PARAM_KEY_YOUTUBE + getResources().getString(R.string.youtube_apikey) + "&" +
            Utils.PARAM_VIDEO_ID_YOUTUBE + mVideoIds;

        JsonObjectRequest request = new JsonObjectRequest(url, null,
            new Response.Listener<JSONObject>() {
                JSONArray dataItemArrays;
                JSONObject itemContentObject;

                @Override
                public void onResponse(JSONObject response) {
                    // To make sure Activity is still in the foreground
                    Activity activity = getActivity();
                    if (activity != null && isAdded()) {
                        try {
                            haveResultView();
                            dataItemArrays = response.getJSONArray(Utils.ARRAY_ITEMS);
                            if (dataItemArrays.length() > 0 && !mTempVideoData.isEmpty()) {
                                for (int i = 0; i < dataItemArrays.length(); i++) {
                                    HashMap<String, String> dataMap = new HashMap<>();

                                    // Detail Array per Item
                                    JSONObject itemsObjects = dataItemArrays.getJSONObject(i);

                                    // Item to get duration
                                    // Item для продолжительности
                                    itemContentObject = itemsObjects.
                                            getJSONObject(Utils.OBJECT_ITEMS_CONTENT_DETAIL);
                                    mDuration = itemContentObject.getString(Utils.KEY_DURATION);

                                    // Convert ISO 8601 time to string
                                    String mDurationInTimeFormat = Utils.
                                            getTimeFromString(mDuration);


                                    // Store titles, video IDs, and thumbnails from mTempVideoData
                                    // to dataMap.
                                    dataMap.put(Utils.KEY_DURATION, mDurationInTimeFormat);
                                    dataMap.put(Utils.KEY_URL_THUMBNAILS,
                                            mTempVideoData.get(i).get(Utils.KEY_URL_THUMBNAILS));
                                    dataMap.put(Utils.KEY_TITLE,
                                            mTempVideoData.get(i).get(Utils.KEY_TITLE));
                                    dataMap.put(Utils.KEY_VIDEO_ID,
                                            mTempVideoData.get(i).get(Utils.KEY_VIDEO_ID));
                                    dataMap.put(Utils.KEY_PUBLISHEDAT,
                                            mTempVideoData.get(i).get(Utils.KEY_PUBLISHEDAT));

                                    // And store dataMap to videoData
                                    mVideoData.add(dataMap);
                                    // Insert 1 by 1 to mAdapter
                                    mAdapterList.notifyItemInserted(mVideoData.size());


                                }

                                // Clear mTempVideoData after it done to insert all in videoData
                                // Очищаем временную переменную mTempVideoData
                                mTempVideoData.clear();
                                mTempVideoData = new ArrayList<>();

                                // Data from server already load all or no data in server
                                // Данные с сервера уже загружены или данных на сервере нет
                            } else {
                                if (mIsAppFirstLaunched &&
                                        mAdapterList.getAdapterItemCount() <= 0) {
                                    noResultView();
                                }
                            }

                        } catch (JSONException e) {
                            Log.d(Utils.TAG_PONGODEV + TAG,
                                    "JSON Parsing error: " + e.getMessage());
                            mPrgLoading.setVisibility(View.GONE);
                        }
                        mPrgLoading.setVisibility(View.GONE);
                    }
                }
            },

            new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                    // To make sure Activity is still in the foreground
                    Activity activity = getActivity();
                    if (activity != null && isAdded()) {
                        Log.d(Utils.TAG_PONGODEV + TAG, "on Error Response get duration: " +
                                error.getMessage());
                        // "try-catch" To handle when still in process and then application closed
                        try {
                            String msgSnackBar;
                            if (error instanceof NoConnectionError) {
                                msgSnackBar = getResources().getString(R.string.no_internet_connection);
                            } else {
                                msgSnackBar = getResources().getString(R.string.response_error);
                            }

                            // To handle when no data in mAdapter and then get error because no
                            // connection or problem in server.
                            if (mVideoData.size() == 0) {
                                retryView();
                                // Condition when loadmore it has data,
                                // when loadmore then get error because no connection.
                            }

                            //Utils.showSnackBar(getActivity(), msgSnackBar);
                            mPrgLoading.setVisibility(View.GONE);

                        } catch (Exception e) {
                            Log.d(Utils.TAG_PONGODEV + TAG, "failed catch volley " + e.toString());
                            mPrgLoading.setVisibility(View.GONE);
                        }
                    }
                }
            }
        );
        request.setRetryPolicy(new DefaultRetryPolicy(Utils.ARG_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        MySingleton.getInstance(getActivity()).getRequestQueue().add(request);

    }

    // Method to hide other view and display retry layout
    // Метод для скрытия другого вида и повторное расположение дисплея
    private void retryView() {
        mLytRetry.setVisibility(View.VISIBLE);
        mUltimateRecyclerView.setVisibility(View.GONE);
        mLblNoResult.setVisibility(View.GONE);
    }

    // Method to display Recyclerview and hide other view
    // Метод выводящий Recyclerview  скрывающий другие view
    private void haveResultView() {
        mLytRetry.setVisibility(View.GONE);
        mUltimateRecyclerView.setVisibility(View.VISIBLE);
        mLblNoResult.setVisibility(View.GONE);
    }

    // Method to display no result view and hide other view
    // Метод выводящие view когда нет никаких результатов и скрывающий остальные view
    private void noResultView() {
        mLytRetry.setVisibility(View.GONE);
        mUltimateRecyclerView.setVisibility(View.GONE);
        mLblNoResult.setVisibility(View.VISIBLE);

    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAdView != null) {
            mAdView.destroy();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mAdView != null) {
            mAdView.resume();
        }
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()){
            case R.id.raisedRetry:
                // Re-load video channel
                // Перезагружаем видеоканал
                mPrgLoading.setVisibility(View.VISIBLE);
                haveResultView();
                getVideoData();
                break;
            default:
                break;
        }
    }
}
