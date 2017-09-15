# RefrashLayout
**example**
   **xml**
   <com.x.leo.refrashviews.RefrashLayout
        android:layout_width="match_parent"
        android:background="@color/colorPrimary"
        android:id = "@+id/rf_myloan"
        xmlns:app="http://schemas.android.com/apk/res-auto"
        app:mainView="@+id/rv_loan"
        app:topView="@+id/top_view"
        android:layout_height="match_parent">
        <TextView
            android:layout_width="match_parent"
            android:id = "@+id/top_view"
            style="@style/text_14dp_white"
            android:text="@string/textview_refrash"
            android:gravity="center"
            android:layout_height="@dimen/dp120" />

        <android.support.v7.widget.RecyclerView
            android:id="@+id/rv"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:layout_gravity="center_horizontal"
            android:paddingTop="@dimen/dp20"
            ></android.support.v7.widget.RecyclerView>

    </com.x.leo.refrashviews.RefrashLayout>
   **java**
    refrashLayout.setOnRefrashListener(new OnRefrashAdapter() {
                @Override
                public void onTopRefrash() {
                    mPresenter.initLoanData();
                }

                @Override
                public void onBottomRefrash() {

                }
            });
