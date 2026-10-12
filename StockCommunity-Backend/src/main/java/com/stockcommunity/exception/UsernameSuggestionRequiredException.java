package com.stockcommunity.exception;

// 註冊時使用者沒填帳號、系統拿 email 自動產生預設帳號，但那個預設帳號剛好被別人用掉時丟出。
// 跟一般的 IllegalArgumentException 不同：這裡不是要使用者「重新輸入」，
// 而是系統已經先想好一個可用的建議帳號（suggestedUsername），
// 前端收到後可以直接把這個建議值顯示出來，讓使用者確認或自己修改，不用從零開始想帳號名稱
public class UsernameSuggestionRequiredException extends RuntimeException {

    private final String suggestedUsername;

    public UsernameSuggestionRequiredException(String suggestedUsername) {
        super("預設帳號名稱已被使用，請確認或修改以下建議的帳號名稱");
        this.suggestedUsername = suggestedUsername;
    }

    public String getSuggestedUsername() {
        return suggestedUsername;
    }
}