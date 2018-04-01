package im.socks.yysk;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import im.socks.yysk.api.YyskApi;
import im.socks.yysk.util.NetUtil;
import im.socks.yysk.util.XBean;
import im.socks.yysk.util.XRspBean;

/**
 * Created by cole on 2017/10/23.
 */

public class ForgetPasswordFragment extends Fragment {
    private EditText phoneNumberView;
    private EditText verifyCodeView;
    private EditText passwordView;

    private Button getVerifyCodeView;
    private Button submitView;

    private final AppDZ app = Yysk.app;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_forget_password, container, false);
        phoneNumberView = view.findViewById(R.id.phoneNumberView);
        verifyCodeView = view.findViewById(R.id.verifyCodeView);
        passwordView = view.findViewById(R.id.passwordView);
        submitView = view.findViewById(R.id.submitView);

        getVerifyCodeView = view.findViewById(R.id.getVerifyCodeView);
        getVerifyCodeView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendVerifyCode();
            }
        });


        //submitView.setEnabled(false);
        submitView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doSubmit();
            }
        });
        return view;
    }

    private void showError(String msg) {
        //或者显示对话框
        Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
    }

    private void sendVerifyCode() {
        String phoneNumber = phoneNumberView.getText().toString();
        //检查手机号是否正确
        getVerifyCodeView.setEnabled(false);

        app.getApi().getVerifyCode(phoneNumber,true,new YyskApi.ICallback<XBean>() {
            @Override
            public void onResult(XBean result) {
                MyLog.d("getVerifyCode=%s",result);
                getVerifyCodeView.setEnabled(true);
                if(NetUtil.checkAndHandleRsp(result,getContext(),"发送验证码失败",null)){
                    showError("发送验证码成功，请查收短信");
                }
            }
        });
    }

    private void doSubmit() {
        final String phoneNumber = phoneNumberView.getText().toString();
        final String newPassword = passwordView.getText().toString();
        String verifyCode = verifyCodeView.getText().toString();

        final ProgressDialog dialog = new ProgressDialog(getContext());
        dialog.setMessage("正在修改密码...");
        dialog.show();
        app.getApi().changePassword(phoneNumber, newPassword, verifyCode, new YyskApi.ICallback<XRspBean>() {
            @Override
            public void onResult(XRspBean result) {
                MyLog.d("changePassword=%s",result);
                if (result != null) {
                    if (result.isEquals("retcode", "succ")) {
                        doLogin(phoneNumber, newPassword, dialog);
                    } else {
                        dialog.dismiss();
                        showError("修改密码失败：" + result.getString("error"));
                    }
                } else {
                    dialog.dismiss();
                    showError("修改密码失败，请检查网络后再次尝试");
                }
            }
        });
    }

    private void doLogin(final String phoneNumber, final String password, final ProgressDialog dialog) {
        boolean autoLogin = true;
        if (autoLogin) {
            dialog.setMessage("正在登录...");
            app.getApi().loginDZ(phoneNumber, password, new YyskApi.ICallback<XBean>() {
                @Override
                public void onResult(XBean result) {
                    //自动登录
                    dialog.dismiss();
                    if (NetUtil.checkAndHandleRsp(result,getContext(),"自动登录失败","请手动登录",null)) {
                        getFragmentStack().show(null, "main", true);
                        app.getSessionManager().onLogin(result, phoneNumber, password);
                    } else {
                        //api错误
                        getFragmentStack().show(LoginFragment.newInstance(null), "login", true);
                    }
                }
            });
        } else {
            //手动登录
            dialog.dismiss();
            getFragmentStack().show(LoginFragment.newInstance(null), "login", true);
        }
    }

    private FragmentStack getFragmentStack() {
        return ((MainActivity) getActivity()).getFragmentStack();
    }

    public static ForgetPasswordFragment newInstance() {
        ForgetPasswordFragment fragment = new ForgetPasswordFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }
}