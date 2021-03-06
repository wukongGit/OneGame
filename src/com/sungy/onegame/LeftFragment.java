package com.sungy.onegame;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler.Callback;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.PlatformActionListener;
import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.framework.Platform.ShareParams;
import cn.sharesdk.framework.utils.UIHandler;
import cn.sharesdk.onekeyshare.OnekeyShare;
import cn.sharesdk.onekeyshare.ShareContentCustomizeCallback;
import cn.sharesdk.sina.weibo.SinaWeibo;
import cn.sharesdk.tencent.qzone.QZone;

import com.sungy.onegame.activity.AboutUsActivity;
import com.sungy.onegame.activity.DetailActivity;
import com.sungy.onegame.activity.FavoritesActivity;
import com.sungy.onegame.activity.FeedBackActivity;
import com.sungy.onegame.mclass.Global;
import com.sungy.onegame.mclass.Global.LoginListener;
import com.sungy.onegame.mclass.HttpUtils;
import com.sungy.onegame.mclass.ToastUtils;

public class LeftFragment extends Fragment implements Callback {
	private TextView userNameTv, userLoginTv;
	private ImageView userImage;
	private final static int WEIBO_NAME = 0;
	private final static int WEIBO_Image = 1;
	private final static int QQ_NAME = 2;
	private final static int QQ_Image = 3;
	public final static String[] plats = { "新浪微博登录", "QQ登录" };
	private Platform qzone;
	private Platform weibo;
	private Context mContext;
	private boolean isDefaultLogin = false;
	
	//字体
    private Typeface tf;
    //字体路径
    private String typeFaceDir = "fonts/font.ttf";
	//星期中文
	private final String[] WEEKS_CN = new String[]{
			"星期日","星期一","星期二","星期三","星期四","星期五","星期六"	
		};
	
	//正在登录对话框
	private AlertDialog dialog;

	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		ShareSDK.initSDK(getActivity());

		//字体
        AssetManager mgr = getActivity().getAssets();//得到AssetManager
        tf = Typeface.createFromAsset(mgr, typeFaceDir);//根据路径得到Typeface
        
		View view = inflater.inflate(R.layout.left_fragment, null);
		userNameTv = (TextView) view.findViewById(R.id.userName);
		userNameTv.setTypeface(tf);
		userImage = (ImageView) view.findViewById(R.id.userImage);
		userLoginTv = (TextView) view.findViewById(R.id.userLogin);
		userLoginTv.setTypeface(tf);
		

		// user login
		LinearLayout oneGameLogin = (LinearLayout) view
				.findViewById(R.id.login);
		oneGameLogin.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (userLoginTv.getText().equals("退出登录")) {
					new AlertDialog.Builder(getActivity())
							.setTitle("提示")
							.setMessage("您确定要注销？")
							.setNegativeButton("取消",
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(
												DialogInterface dialog,
												int which) {
										}
									})
							.setPositiveButton("确定",
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog,
												int whichButton) {
											String[] infos = readLoginInfo();
											clearLoginInfo();
											clearGlobal();
											String plat = infos[3];
											if (plat.equals("qq")) {
												qzone = ShareSDK.getPlatform(
														getActivity(),
														QZone.NAME);
											} else if (plat.equals("weibo")) {
												weibo = ShareSDK.getPlatform(
														getActivity(),
														SinaWeibo.NAME);
											}
											userNameTv.setText("name");
											userLoginTv.setText("登录");
											userImage
													.setImageResource(R.drawable.defaultimage);
											ToastUtils.showDefaultToast(
													getActivity(), "注销成功",
													Toast.LENGTH_SHORT);
											isDefaultLogin = false;
										}
									}).show();
				} else {
					AlertDialog.Builder mBuilder = new AlertDialog.Builder(
							getActivity());
					mBuilder.setTitle("选择登录平台").setItems(plats,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									switch (which) {
									case 0:
										toWeibo();
										break;
									case 1:
										toQQ();
										break;
									}
								}
							});
					mBuilder.show();
				}
			}
		});

		//每日一游
		LinearLayout oneGamePage = (LinearLayout) view
				.findViewById(R.id.one_game);
		oneGamePage.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				FragmentTransaction ft = getActivity()
						.getSupportFragmentManager().beginTransaction();
//				ft.replace(R.id.center_frame, new SampleListFragment());
				ft.replace(R.id.center_frame, ((MainActivity)getActivity()).getCenterFragment());
				ft.commit();
				((MainActivity) getActivity()).showLeft();
			}
		});

//		LinearLayout oneResourcePage = (LinearLayout) view
//				.findViewById(R.id.one_resource);
//		oneResourcePage.setOnClickListener(new View.OnClickListener() {
//
//			public void onClick(View v) {
//				ResourceFragment resource = new ResourceFragment();
//				FragmentTransaction ft = getActivity()
//						.getSupportFragmentManager().beginTransaction();
//				ft.replace(R.id.center_frame, resource);
//				ft.commit();
//				((MainActivity) getActivity()).showLeft();
//			}
//		});

		//我的收藏
		LinearLayout oneFavoritesPage = (LinearLayout) view
				.findViewById(R.id.one_favorite);
		oneFavoritesPage.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				// 检查是否登录
				if (!Global.checkLogin(getActivity())) {
					return;
				}
				Intent i = new Intent(getActivity(), FavoritesActivity.class);
				startActivity(i);
			}
		});

		//关于我们
		LinearLayout oneAboutUsPage = (LinearLayout) view.findViewById(R.id.one_about);
		oneAboutUsPage.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent=new Intent(getActivity(), AboutUsActivity.class);
				startActivity(intent);
			}
		});
		
		//推荐我们
		LinearLayout oneRecommendPage=(LinearLayout) view.findViewById(R.id.one_recommend);
		oneRecommendPage.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// 检查是否登录
				if (!Global.checkLogin(getActivity())) {
					return;
				}
				LayoutInflater factory = LayoutInflater.from(getActivity());
				View DialogView = factory.inflate(R.layout.recommend_layout, null);
				//当天时间
				Calendar c = Calendar.getInstance();
		        int mMonth = c.get(Calendar.MONTH);//获取当前月份
		        int mDay = c.get(Calendar.DAY_OF_MONTH);//获取当前月份的日期号码
		        int mWeek = c.get(Calendar.DAY_OF_WEEK);
				String showTime = "";
				showTime = (mMonth+1)+"月"+mDay+"日	"+WEEKS_CN[mWeek-1];
				
				TextView time = (TextView)DialogView.findViewById(R.id.time);
				time.setText(showTime);
				time.setTypeface(tf);
				
				final TextView recomment_text = (TextView)DialogView.findViewById(R.id.recomment_text);
				recomment_text.setTypeface(tf);
				
				final TextView mEditText=(TextView) DialogView.findViewById(R.id.comment);
				mEditText.setTypeface(tf);
				
				final AlertDialog dlg = new AlertDialog.Builder(getActivity())
		    	.setView(DialogView)
				.create();
		    	dlg.show();
		    	
		    	//完成推荐我们
		    	ImageView recommendBtn=(ImageView) DialogView.findViewById(R.id.recommend_btn);
		    	recommendBtn.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						shareRecommend();
						dlg.dismiss();
					}
				});
		    	//取消推荐我们
		    	ImageView recommendCancel=(ImageView) DialogView.findViewById(R.id.recommend_cancel);
		    	recommendCancel.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						dlg.dismiss();
					}
				});
			}
		});
		
		//反馈
		LinearLayout oneFeedbackPage = (LinearLayout) view
				.findViewById(R.id.one_feedback);
		oneFeedbackPage.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				startActivity(new Intent(getActivity(), FeedBackActivity.class));
			}
		});

		return view;
	}

	public void onActivityCreated(Bundle savedInstanceState) {

		super.onActivityCreated(savedInstanceState);
		ShareSDK.initSDK(getActivity());

		mContext = getActivity();

		// 初始化登录信息
		initLoginInfo();
	}

	// 初始化登录信息
	private void initLoginInfo() {
		String username = "";
		String userimage = "";
		String userid = "";
		String plat = "";
		String thirdid = "";
		String[] infos = null;
		// 写入操作,并授权
		infos = readLoginInfo();
		if (infos != null) {
			isDefaultLogin = true;

			username = infos[0];
			userimage = infos[1];
			userid = infos[2];
			plat = infos[3];
			thirdid = infos[4];
			afterLogin(username, plat, thirdid, userimage);
			if (plat.equals("qq")) {
				qzone = ShareSDK.getPlatform(getActivity(), QZone.NAME);
				qzone.SSOSetting(true);
				// qzone.authorize();
			} else if (plat.equals("weibo")) {
				weibo = ShareSDK.getPlatform(getActivity(), SinaWeibo.NAME);
				weibo.SSOSetting(true);
				// weibo.authorize();
			}
		}
	}

	private Bitmap downloadIcon(String url) {
		try {
			URL mUrl = new URL(url);
			HttpURLConnection mConnection = (HttpURLConnection) mUrl
					.openConnection();
			InputStream mStream = mConnection.getInputStream();
			Bitmap bitmap = BitmapFactory.decodeStream(mStream);
			return bitmap;
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public boolean handleMessage(Message msg) {
		switch (msg.what) {
		case WEIBO_NAME:
			userNameTv.setText((String) msg.obj);
			userLoginTv.setText("退出登录");
			tipLoginSuccess((String) msg.obj);
			
			//去掉正在登录对话框
			if(dialog	!=	null){
				dialog.dismiss();
			}
			break;
		case WEIBO_Image:
			userImage.setImageBitmap((Bitmap) msg.obj);
			break;
		case QQ_NAME:
			userNameTv.setText((String) msg.obj);
			userLoginTv.setText("退出登录");
			tipLoginSuccess((String) msg.obj);
			
			//去掉正在登录对话框
			if(dialog	!=	null){
				dialog.dismiss();
			}
			break;
		case QQ_Image:
			userImage.setImageBitmap((Bitmap) msg.obj);
			break;
		default:
			break;
		}
		return false;
	}

	// 提示登录成功
	private void tipLoginSuccess(String name) {
		Context context = mContext;
		// 是否是其他页面登录
		if (isFromOther) {
			context = otherContext;
		}
		if (!isDefaultLogin) {
			new AlertDialog.Builder(context)
					.setTitle("每日游")
					.setMessage("欢迎来到每日游OneGame," + name + "!")
					.setPositiveButton("确定",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
								}
							}).show();
		}
		setIsFromOther(false);
	}



	// 接入微博
	public void toWeibo() {
		if (!HttpUtils.isNetworkConnected(getActivity())) {
			ToastUtils.showCenterToast(getActivity(), "请检查网络状态", Toast.LENGTH_SHORT);
			return;
		}
		weibo = ShareSDK.getPlatform(getActivity(), SinaWeibo.NAME);
		weibo.SSOSetting(true);
		weibo.setPlatformActionListener(new PlatformActionListener() {

			@Override
			public void onError(Platform arg0, int arg1, Throwable arg2) {

			}

			@Override
			public void onComplete(Platform arg0, int arg1,
					HashMap<String, Object> arg2) {
				final String name = arg0.getDb().getUserName();
				final String userId = arg0.getDb().getUserId();
				final String iconUrl = arg0.getDb().getUserIcon();
				
				afterLogin(name, "weibo", userId, iconUrl);
				
			}

			@Override
			public void onCancel(Platform arg0, int arg1) {
				//去掉正在登录对话框
				if(dialog	!=	null){
					dialog.dismiss();
				}
			}
		});
		
		weibo.authorize();
		popLoadingDialog();
	}

	// 接入QQ
	public void toQQ() {
		if (!HttpUtils.isNetworkConnected(getActivity())) {
			ToastUtils.showCenterToast(getActivity(), "请检查网络状态", Toast.LENGTH_SHORT);
			return;
		}
		qzone = ShareSDK.getPlatform(getActivity(), QZone.NAME);
		qzone.SSOSetting(true);
		qzone.setPlatformActionListener(new PlatformActionListener() {

			@Override
			public void onError(Platform arg0, int arg1, Throwable arg2) {

			}

			@Override
			public void onComplete(Platform arg0, int arg1,
					HashMap<String, Object> arg2) {
				final String name = arg0.getDb().getUserName();
				final String userId = arg0.getDb().getUserId();
				final String iconUrl = arg0.getDb().getUserIcon();

				afterLogin(name, "qq", userId, iconUrl);
				
			}

			@Override
			public void onCancel(Platform arg0, int arg1) {
				//去掉正在登录对话框
				if(dialog	!=	null){
					dialog.dismiss();
				}
			}
		});
		qzone.authorize();
		popLoadingDialog();
	}
	
	private void shareRecommend(){
		ShareSDK.initSDK(getActivity());
		OnekeyShare oks = new OnekeyShare();
 
		// 分享时Notification的图标和文字，不必改
		oks.setNotification(R.drawable.ic_launcher,
				getString(R.string.app_name));
		// title标题，QQ空间一定要用
		oks.setTitle("每日游推广");
		// // titleUrl是标题的网络链接，QQ空间一定要用
		oks.setTitleUrl("http://sharesdk.cn");
		// text是分享文本，所有平台都需要这个字段
		oks.setText("您在每日游支持的每一个游戏，都在构筑着我们的游戏世界。每日游，专注游戏每一天，快乐游戏到永远。点击链接下载每日游，畅想精致游戏，玩转您的游戏世界:www.baidu.com\n");

//		 site是分享此内容的网站名称，QQ空间一定要用
		oks.setSite(getString(R.string.app_name));
		 // siteUrl是分享此内容的网站地址，QQ空间一定要用
		oks.setSiteUrl("http://sharesdk.cn");
		oks.setSilent(true);

		// 去除注释可通过OneKeyShareCallback来捕获快捷分享的处理结果
		// 通过OneKeyShareCallback来修改不同平台分享的内容
		oks.setShareContentCustomizeCallback(new ShareContentCustomizeCallback() {

			@Override
			public void onShare(Platform platform, ShareParams paramsToShare) {
				
			}
			
		});
		oks.setCallback(new PlatformActionListener() {
			
			@Override
			public void onError(Platform arg0, int arg1, Throwable arg2) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onComplete(Platform arg0, int arg1, HashMap<String, Object> arg2) {
				
			}
			
			@Override
			public void onCancel(Platform arg0, int arg1) {
				// TODO Auto-generated method stub
				
			}
		});
		oks.show(getActivity());
	}
	
	//弹出加载中对话框
	private void popLoadingDialog(){
		Context context = mContext;
		// 是否是其他页面登录
		if (isFromOther) {
			context = otherContext;
		}
		if (!isDefaultLogin) {
			LayoutInflater factory = LayoutInflater.from(context);
			View DialogView = factory.inflate(R.layout.loading_dialog, null);
			dialog = new AlertDialog.Builder(context)
					.setTitle("登录加载..")
					.setView(DialogView).show();
		}
		setIsFromOther(false);
	}

	// 写入全局数据
	public void write2Global(String user_id, String name, String iconUrl,
			boolean isLogin) {
		Global.setUserId(user_id);
		Global.setUserNmae(name);
		Global.setUserImage(iconUrl);
		Global.setLogin(isLogin);
	}

	// 消除全局数据
	public void clearGlobal() {
		Global.setUserId("");
		Global.setUserNmae("");
		Global.setUserImage("");
		Global.setLogin(false);
	}

	// 获取登录信息
	private String[] readLoginInfo() {
		// 获取到sharepreference 对象， 参数一为xml文件名，参数为文件的可操作模式
		SharedPreferences sp = getActivity().getApplicationContext()
				.getSharedPreferences("onegameLoginInfo",
						Context.MODE_WORLD_READABLE);
		if (sp.getString("username", "").equals("")) {
			return null;
		} else {
			String[] infos = new String[5];
			infos[0] = sp.getString("username", "");
			infos[1] = sp.getString("userimage", "");
			infos[2] = sp.getString("userid", "");
			infos[3] = sp.getString("plat", "");
			infos[4] = sp.getString("thirdid", "");
			return infos;
		}
	}

	// 写入登录信息
	private void writeLoginInfo(String username, String userimage,
			String userid, String plat, String thirdid) {
		// 获取到sharepreference 对象， 参数一为xml文件名，参数为文件的可操作模式
		SharedPreferences sp = getActivity().getApplicationContext()
				.getSharedPreferences("onegameLoginInfo",
						Context.MODE_WORLD_WRITEABLE);
		SharedPreferences.Editor edit = sp.edit();
		edit.putString("username", username);
		edit.putString("userimage", userimage);
		edit.putString("userid", userid);
		edit.putString("plat", plat);
		edit.putString("thirdid", thirdid);
		// 提交
		edit.commit();
	}

	// 消除登录信息
	private void clearLoginInfo() {
		// 获取到sharepreference 对象， 参数一为xml文件名，参数为文件的可操作模式
		SharedPreferences sp = getActivity().getApplicationContext()
				.getSharedPreferences("onegameLoginInfo",
						Context.MODE_WORLD_WRITEABLE);
		SharedPreferences.Editor edit = sp.edit();
		edit.putString("username", "");
		edit.putString("userimage", "");
		edit.putString("userid", "");
		edit.putString("plat", "");
		edit.putString("thirdid", "");
		// 提交
		edit.commit();
	}
	
	// 登录后的操作
	private void afterLogin(final String name, final String plat,
			final String thirdId, final String iconUrl) {
		new Thread() {
			public void run() {
				List<NameValuePair> params = new ArrayList<NameValuePair>();
				params.add(new BasicNameValuePair("user_name", name));
				params.add(new BasicNameValuePair("third_plat", plat));
				params.add(new BasicNameValuePair("third_id", thirdId));
				params.add(new BasicNameValuePair("user_image", iconUrl));
				String str = HttpUtils.doPostWithoutStrict(Global.USER_THIRDLOGIN, params);

				// 写入全局数据
				JSONObject json;
				String user_id = "";
				try {
					json = new JSONObject(str);
					user_id = json.getString("user_id");
					write2Global(user_id, name, iconUrl, true);
					// 写入sharepreference
					writeLoginInfo(name, iconUrl, user_id, plat, thirdId);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				Bitmap mBitmap = downloadIcon(iconUrl);

				Message msg = new Message();
				msg.what = (plat.equals("qq")) ? QQ_NAME : WEIBO_NAME;
				msg.obj = name;
				UIHandler.sendMessage(msg, LeftFragment.this);

				Message msg2 = new Message();
				msg2.what = (plat.equals("qq")) ? QQ_Image : WEIBO_Image;
				msg2.obj = mBitmap;
				UIHandler.sendMessage(msg2, LeftFragment.this);

			};
		}.start();

	}

	// 为其他页面登录所用
	public boolean isFromOther = false;
	public Context otherContext;
	// 为其他页面登录所用
	public Global.LoginListener listener;

	// 设置listner
	public void setListener() {
		listener = new LoginListener() {
			@Override
			public void clickToQQ(Context context) {
				setOtherContext(context);
				setIsFromOther(true);
				toQQ();
			}

			@Override
			public void clickToWeibo(Context context) {
				setOtherContext(context);
				setIsFromOther(true);
				toWeibo();
			}
		};
	}

	public Global.LoginListener getListner() {
		return listener;
	}

	public void setOtherContext(Context context) {
		otherContext = context;
	}

	public void setIsFromOther(boolean is) {
		isFromOther = is;
	}
}
