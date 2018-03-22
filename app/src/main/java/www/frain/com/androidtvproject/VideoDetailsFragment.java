/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package www.frain.com.androidtvproject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v17.leanback.app.DetailsFragment;
import android.support.v17.leanback.app.DetailsFragmentBackgroundController;
import android.support.v17.leanback.app.ProgressBarManager;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.DetailsOverviewRow;
import android.support.v17.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import android.support.v17.leanback.widget.FullWidthDetailsOverviewSharedElementHelper;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.orhanobut.logger.Logger;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/*
 * LeanbackDetailsFragment extends DetailsFragment, a Wrapper fragment for leanback details screens.
 * It shows a detailed view of video and its meta plus related videos.
 */
public class VideoDetailsFragment extends DetailsFragment {
    private static final String TAG = "VideoDetailsFragment";

    private static final int ACTION_WATCH_TRAILER = 1;
    private static final int ACTION_WATCH_DRAMA = 4;
    private static final int ACTION_RENT = 2;
    private static final int ACTION_BUY = 3;
    private static final int ACTION_WATCH_REFRESH = 5;

    private static final int DETAIL_THUMB_WIDTH = 274;
    private static final int DETAIL_THUMB_HEIGHT = 274;

    private static final int NUM_COLS = 10;

    private Movie mSelectedMovie;

    private ArrayObjectAdapter mAdapter;
    private ClassPresenterSelector mPresenterSelector;

    private DetailsFragmentBackgroundController mDetailsBackground;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate DetailsFragment");
        super.onCreate(savedInstanceState);

        mDetailsBackground = new DetailsFragmentBackgroundController(this);

        mSelectedMovie =
                (Movie) getActivity().getIntent().getSerializableExtra(DetailsActivity.MOVIE);
        if (mSelectedMovie != null) {
            mPresenterSelector = new ClassPresenterSelector();
            mAdapter = new ArrayObjectAdapter(mPresenterSelector);
            setupDetailsOverviewRow();
            setupDetailsOverviewRowPresenter();
            setupRelatedMovieListRow();
            setAdapter(mAdapter);
            getVideoDetials();
            initializeBackground(mSelectedMovie);
            setOnItemViewClickedListener(new ItemViewClickedListener());

        } else {
            Intent intent = new Intent(getActivity(), MainActivity.class);
            startActivity(intent);
        }
    }

    private void initializeBackground(Movie data) {
        mDetailsBackground.enableParallax();
        Glide.with(getActivity())
                .load(data.getBackgroundImageUrl())
                .asBitmap()
                .centerCrop()
                .error(R.drawable.default_background)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap bitmap,
                                                GlideAnimation<? super Bitmap> glideAnimation) {
                        mDetailsBackground.setCoverBitmap(bitmap);
                        mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size());
                    }
                });
    }

    private void setupDetailsOverviewRow() {
        Log.d(TAG, "doInBackground: " + mSelectedMovie.toString());
        final DetailsOverviewRow row = new DetailsOverviewRow(mSelectedMovie);
        row.setImageDrawable(
                ContextCompat.getDrawable(getActivity(), R.drawable.default_background));
        int width = convertDpToPixel(getActivity().getApplicationContext(), DETAIL_THUMB_WIDTH);
        int height = convertDpToPixel(getActivity().getApplicationContext(), DETAIL_THUMB_HEIGHT);
        Glide.with(getActivity())
                .load(mSelectedMovie.getCardImageUrl())
                .centerCrop()
                .error(R.drawable.default_background)
                .into(new SimpleTarget<GlideDrawable>(width, height) {
                    @Override
                    public void onResourceReady(GlideDrawable resource,
                                                GlideAnimation<? super GlideDrawable>
                                                        glideAnimation) {
                        Log.d(TAG, "details overview card image url ready: " + resource);
                        row.setImageDrawable(resource);
                        mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size());
                    }
                });

        ArrayObjectAdapter actionAdapter = new ArrayObjectAdapter();

        actionAdapter.add(
                new Action(
                        ACTION_WATCH_TRAILER,
                        getResources().getString(R.string.watch_trailer_1),
                        getResources().getString(R.string.watch_trailer_2)));
        actionAdapter.add(
                new Action(
                        ACTION_WATCH_REFRESH,
                        getResources().getString(R.string.watch_refresh),
                        getResources().getString(R.string.watch_trailer_2)));
        row.setActionsAdapter(actionAdapter);

//        GridItemPresenter mGridPresenter = new GridItemPresenter();
//        ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(mGridPresenter);
//        gridRowAdapter.add(getResources().getString(R.string.grid_view));
//        gridRowAdapter.add(getString(R.string.error_fragment));
//        gridRowAdapter.add(getResources().getString(R.string.personal_settings));


        mAdapter.add(row);
//        mAdapter.add(new ListRow(gridRowAdapter));
    }

    private static final int GRID_ITEM_WIDTH = 200;
    private static final int GRID_ITEM_HEIGHT = 200;

    private class GridItemPresenter extends Presenter {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            TextView view = new TextView(parent.getContext());
            view.setLayoutParams(new ViewGroup.LayoutParams(GRID_ITEM_WIDTH, GRID_ITEM_HEIGHT));
            view.setFocusable(true);
            view.setFocusableInTouchMode(true);
            view.setBackgroundColor(getResources().getColor(R.color.default_background));
            view.setTextColor(Color.WHITE);
            view.setGravity(Gravity.CENTER);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item) {
            ((TextView) viewHolder.view).setText((String) item);
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {
        }
    }

    private void setupDetailsOverviewRowPresenter() {
        // Set detail background.
        FullWidthDetailsOverviewRowPresenter detailsPresenter =
                new FullWidthDetailsOverviewRowPresenter(new DetailsDescriptionPresenter());
        detailsPresenter.setBackgroundColor(
                ContextCompat.getColor(getActivity(), R.color.selected_background));

        // Hook up transition element.
        FullWidthDetailsOverviewSharedElementHelper sharedElementHelper =
                new FullWidthDetailsOverviewSharedElementHelper();
        sharedElementHelper.setSharedElementEnterTransition(
                getActivity(), DetailsActivity.SHARED_ELEMENT_NAME);
        detailsPresenter.setListener(sharedElementHelper);
        detailsPresenter.setParticipatingEntranceTransition(true);
        detailsPresenter.setOnActionClickedListener(new OnActionClickedListener() {
            @Override
            public void onActionClicked(Action action) {
                if (action.getId() == ACTION_WATCH_TRAILER) {
                    if (!fristUrl.equals("")) {
                        Log.d(TAG, "续播: " + fristUrl);
                        Intent intent = new Intent(getActivity(), SimplePlayActivity.class);
                        intent.putExtra(DetailsActivity.URL, fristUrl);
                        startActivity(intent);
                    }
                } else if (action.getId() == ACTION_WATCH_REFRESH) {
//                    Logger.i("被点击了", "======>" + action.getLabel1());
//                    if (list == null || list.size() <= 0) {
//                        Toast.makeText(getActivity(), "刷新剧集中...", Toast.LENGTH_SHORT).show();
//                        Log.d(TAG, "刷新剧集中...11111111111");
//
//                        getVideoDetials();
//                    }else{
//                        Log.d(TAG, "正在加載或加載失敗...11111111111");
//
//                    }
                } else {
                    Toast.makeText(getActivity(), action.toString(), Toast.LENGTH_SHORT).show();
                }


            }
        });
        mPresenterSelector.addClassPresenter(DetailsOverviewRow.class, detailsPresenter);
    }
    AlertDialog alertDialog;
    void getVideoDetials() {
        if(alertDialog==null) {
            alertDialog = new AlertDialog.Builder(getActivity()).setMessage("加载中...").create();
        }else{
            alertDialog.setMessage("重新加载中...");
        }
        alertDialog.show();
        switch (mSelectedMovie.getType()) {
            case Movie.TYPE_HJ:
                getVideoUrl();
                break;
            case Movie.TYPE_MJ:
                getMJUrl();
                break;
        }
    }

    void getMJUrl() {
        new Thread() {
            @Override
            public void run() {
                try {
                    String url = mSelectedMovie.getVideoUrl();
                    Document doc = Jsoup.connect(url).get();
                    Logger.i(doc.toString());
                } catch (IOException e) {
                    Logger.i(e.toString());
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }.start();
    }

    String fristUrl = "";

    void getVideoUrl() {
        new ListTask().execute();
    }

    ArrayList<Drama> list;

    public class ListTask extends AsyncTask<Void, String, ArrayList<Drama>> {

        @Override
        protected ArrayList<Drama> doInBackground(Void... voids) {
            try {
                String url = mSelectedMovie.getVideoUrl();
                Document doc = Jsoups.connect(url);
                Logger.d(TAG, doc.toString());
                Elements elements = doc.select("div.tv-info-list").select("ul.clearfix");
                list = new ArrayList<>();
                for (Element e : elements.select("li")) {
                    String id = e.select("a").select("span.num").text();
                    String videoUrl = e.select("a").attr("href");
                    Document dc = Jsoup.connect("https://www.hanjutv.com" + videoUrl).get();
                    String testUrl = dc.select("iframe#playerIframe").attr("src");
                    Logger.i(testUrl);
                    String _path = testUrl.substring(testUrl.indexOf("path="));
                    Logger.i(_path);
                    list.add(new Drama(id, id, _path));
                }
                fristUrl = list.get(0).getUrl();
                return list;
            } catch (IOException e) {
                e.printStackTrace();
                Logger.e(TAG, e.toString());

            } catch (Exception e) {
                e.printStackTrace();
                Logger.e(TAG, e.toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<Drama> list) {
            if(list!=null){
                alertDialog.dismiss();
                GridItemPresenter mGridPresenter = new GridItemPresenter();
                ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(mGridPresenter);
                for (Drama drama : list) {
                    gridRowAdapter.add(drama.getId());
                }
                mAdapter.add( new ListRow(gridRowAdapter));
            }else{
                alertDialog.setMessage("加载失败");
                getVideoDetials();
            }
            super.onPostExecute(list);
        }
    }

    public class UrlTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... strings) {
            try {
                Connection connect = Jsoup.connect("https://ww4.hanjutv.com/index.php?c=api&" + strings[0]).timeout(10000);
                Map<String, String> header = new HashMap<String, String>();
                header.put(":authority", "ww4.hanjutv.com");
                header.put(":method", "GET");
                header.put(":path", "/index.php?c=api&" + strings[0]);
                header.put(":scheme", "https");
                header.put("accept", "application/json, text/javascript, */*; q=0.01");
                header.put("accept-encoding", "gzip, deflate, br");
                header.put("accept-language", "zh-CN,zh;q=0.9");
                header.put("cache-control", "no-cache");
                header.put("accept", "ww4.hanjutv.com");
                Connection data = connect.data(header);
                Document  document = data.get();
                String result = document.select("body").text();
                JSONObject jsonObject = new JSONObject(result);
                JSONObject dataObject = jsonObject.getJSONObject("data");
                String baiduUrl = dataObject.getString("url");
                return baiduUrl;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            if(s!=null){
                Log.d(TAG,s);
                alertDialog.dismiss();
                Intent intent = new Intent(getActivity(), SimplePlayActivity.class);
                intent.putExtra(DetailsActivity.URL, s);
                startActivity(intent);
            }
            super.onPostExecute(s);
        }
    }


    class Drama {
        String id;
        String name;
        String url;

        public Drama(String id, String name, String url) {
            setId(id);
            setName(name);
            setUrl(url);
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }


    private void setupRelatedMovieListRow() {
        String subcategories[] = {getString(R.string.related_movies)};
        List<Movie> list = MovieList.getList();

        Collections.shuffle(list);
        ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new CardPresenter());
        for (int j = 0; j < NUM_COLS; j++) {
            listRowAdapter.add(list.get(j % 5));
        }

        HeaderItem header = new HeaderItem(0, subcategories[0]);
        mAdapter.add(new ListRow(header, listRowAdapter));
        mPresenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());
    }

    public int convertDpToPixel(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {


            if (!item.toString().equals("刷新") && !item.toString().equals("续播")) {
                Integer integer = Integer.parseInt(item.toString());
                Intent intent;
                Drama drama = list.get(integer - 1);
                switch (mSelectedMovie.getType()) {
                    case Movie.TYPE_HJ:
                        alertDialog.setMessage("正在解析视频源...");
                        alertDialog.show();
                        new UrlTask().execute(drama.getUrl());
                        break;
                    case Movie.TYPE_MJ:
                        intent = new Intent(getActivity(), DetailsActivity.class);
                        mSelectedMovie.setVideoUrl(drama.getUrl());
                        intent.putExtra(getResources().getString(R.string.movie), mSelectedMovie);

                        Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                                getActivity(),
                                ((ImageCardView) itemViewHolder.view).getMainImageView(),
                                DetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                        getActivity().startActivity(intent, bundle);
                        break;
                }

            }
            if (item.toString().equals("刷新")) {
                if (list == null || list.size() <= 0) {
                    Log.d(TAG, "刷新剧集中...22222222222");
                    Toast.makeText(getActivity(), "刷新剧集中...", Toast.LENGTH_SHORT).show();
                    getVideoDetials();
                } else {
                    Log.d(TAG, "正在加載或加載失敗...11111111111");

                }
            }
            if (item.toString().equals("续播") && !fristUrl.equals("")) {
                Log.d(TAG, "续播: " + fristUrl);
                Intent intent = new Intent(getActivity(), SimplePlayActivity.class);
                intent.putExtra(DetailsActivity.URL, fristUrl);
                startActivity(intent);
                Log.d(TAG, "Item: " + item.toString());
            }


        }
    }
}
