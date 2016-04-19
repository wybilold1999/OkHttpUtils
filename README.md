# OkHttpUtils
对OKHttp的封装以及demo

主要是OkHttpUtils类；
对于get请求，若返回String字符串，首先是将泛型指定为String,接着获取完整的url,
然后实例化ResultCallback对象
OkHttpUtils.ResultCallback<String> callBack = new OkHttpUtils.ResultCallback<File>(){

	@Override
	public void onSuccess(String response) {
		Log.d("test", "成功");
	}

	@Override
	public void onFailure(Exception e) {
		Log.d("test", "失败");
	}

	@Override
	public void onProgress(float progress, long total) {
		Log.d("test", String.valueOf(total) + "=" + String.valueOf(progress));
	}
	};
最后直接调用OkHttpUtils.get(url, callBack)即可。

如果是需要下载文件，需要将泛型指定为File类型，然后调用OkHttpUtils.downloadFile(url, callBack)方法即可

如果是post请求，需要将参数封装在Param中，然后调用OkHttpUtils.post(String url, 
ResultCallback callback, List<Param> params)接口即可