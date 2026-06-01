package ru.stepanov.selfcontrol.common;
public record ApiResponse<T>(T data,String message) { public static <T> ApiResponse<T> ok(T data){return new ApiResponse<>(data,null);} public static ApiResponse<Void> message(String message){return new ApiResponse<>(null,message);} }
