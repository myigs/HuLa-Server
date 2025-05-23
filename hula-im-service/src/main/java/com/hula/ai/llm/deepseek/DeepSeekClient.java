package com.hula.ai.llm.deepseek;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hula.ai.llm.deepseek.constant.DeepSeekConst;
import com.hula.ai.llm.openai.OpenAiApi;
import com.hula.ai.llm.openai.entity.Tts.TextToSpeech;
import com.hula.ai.llm.openai.entity.assistant.Assistant;
import com.hula.ai.llm.openai.entity.assistant.AssistantFile;
import com.hula.ai.llm.openai.entity.assistant.AssistantFileResponse;
import com.hula.ai.llm.openai.entity.assistant.AssistantListResponse;
import com.hula.ai.llm.openai.entity.assistant.AssistantResponse;
import com.hula.ai.llm.openai.entity.assistant.message.MessageFileResponse;
import com.hula.ai.llm.openai.entity.assistant.message.MessageResponse;
import com.hula.ai.llm.openai.entity.assistant.message.ModifyMessage;
import com.hula.ai.llm.openai.entity.assistant.run.ModifyRun;
import com.hula.ai.llm.openai.entity.assistant.run.Run;
import com.hula.ai.llm.openai.entity.assistant.run.RunResponse;
import com.hula.ai.llm.openai.entity.assistant.run.RunStepResponse;
import com.hula.ai.llm.openai.entity.assistant.run.ThreadRun;
import com.hula.ai.llm.openai.entity.assistant.run.ToolOutputBody;
import com.hula.ai.llm.openai.entity.assistant.thread.ModifyThread;
import com.hula.ai.llm.openai.entity.assistant.thread.Thread;
import com.hula.ai.llm.openai.entity.assistant.thread.ThreadMessage;
import com.hula.ai.llm.openai.entity.assistant.thread.ThreadResponse;
import com.hula.ai.llm.openai.entity.billing.BillingUsage;
import com.hula.ai.llm.openai.entity.billing.CreditGrantsResponse;
import com.hula.ai.llm.openai.entity.billing.Subscription;
import com.hula.ai.llm.openai.entity.chat.*;
import com.hula.ai.llm.openai.entity.common.DeleteResponse;
import com.hula.ai.llm.openai.entity.common.OpenAiResponse;
import com.hula.ai.llm.openai.entity.common.PageRequest;
import com.hula.ai.llm.openai.entity.completions.Completion;
import com.hula.ai.llm.openai.entity.completions.CompletionResponse;
import com.hula.ai.llm.openai.entity.edits.Edit;
import com.hula.ai.llm.openai.entity.edits.EditResponse;
import com.hula.ai.llm.openai.entity.embeddings.Embedding;
import com.hula.ai.llm.openai.entity.embeddings.EmbeddingResponse;
import com.hula.ai.llm.openai.entity.engines.Engine;
import com.hula.ai.llm.openai.entity.files.File;
import com.hula.ai.llm.openai.entity.files.UploadFileResponse;
import com.hula.ai.llm.openai.entity.fineTune.Event;
import com.hula.ai.llm.openai.entity.fineTune.FineTune;
import com.hula.ai.llm.openai.entity.fineTune.FineTuneDeleteResponse;
import com.hula.ai.llm.openai.entity.fineTune.FineTuneResponse;
import com.hula.ai.llm.openai.entity.fineTune.job.FineTuneJob;
import com.hula.ai.llm.openai.entity.fineTune.job.FineTuneJobEvent;
import com.hula.ai.llm.openai.entity.fineTune.job.FineTuneJobListResponse;
import com.hula.ai.llm.openai.entity.fineTune.job.FineTuneJobResponse;
import com.hula.ai.llm.openai.entity.images.Image;
import com.hula.ai.llm.openai.entity.images.ImageEdit;
import com.hula.ai.llm.openai.entity.images.ImageResponse;
import com.hula.ai.llm.openai.entity.images.ImageVariations;
import com.hula.ai.llm.openai.entity.images.Item;
import com.hula.ai.llm.openai.entity.models.Model;
import com.hula.ai.llm.openai.entity.models.ModelResponse;
import com.hula.ai.llm.openai.entity.moderations.Moderation;
import com.hula.ai.llm.openai.entity.moderations.ModerationResponse;
import com.hula.ai.llm.openai.entity.whisper.Transcriptions;
import com.hula.ai.llm.openai.entity.whisper.Translations;
import com.hula.ai.llm.openai.entity.whisper.WhisperResponse;
import com.hula.ai.llm.openai.exception.BaseException;
import com.hula.ai.llm.openai.exception.CommonError;
import com.hula.ai.llm.openai.function.KeyRandomStrategy;
import com.hula.ai.llm.openai.function.KeyStrategyFunction;
import com.hula.ai.llm.openai.interceptor.DefaultOpenAiAuthInterceptor;
import com.hula.ai.llm.openai.interceptor.DynamicKeyOpenAiAuthInterceptor;
import com.hula.ai.llm.openai.interceptor.OpenAiAuthInterceptor;
import com.hula.ai.llm.openai.plugin.PluginAbstract;
import com.hula.ai.llm.openai.plugin.PluginParam;
import com.hula.ai.llm.openai.utils.SSEUtil;
import io.reactivex.Single;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.sse.EventSourceListener;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 描述： open ai 客户端
 */
@Slf4j
public class DeepSeekClient {
    /**
     * keys
     */
    @Getter
    @NotNull
    private List<String> apiKey;
    /**
     * 自定义api host使用builder的方式构造client
     */
    @Getter
    private String apiHost;
    @Getter
    private OpenAiApi openAiApi;
    /**
     * 自定义的okHttpClient
     * 如果不自定义 ，就是用sdk默认的OkHttpClient实例
     */
    @Getter
    private OkHttpClient okHttpClient;
    /**
     * api key的获取策略
     */
    @Getter
    private KeyStrategyFunction<List<String>, String> keyStrategy;

    /**
     * 自定义鉴权处理拦截器<br/>
     * 可以不设置，默认实现：DefaultOpenAiAuthInterceptor <br/>
     * 如需自定义实现参考：DealKeyWithOpenAiAuthInterceptor
     *
     * @see DynamicKeyOpenAiAuthInterceptor
     * @see DefaultOpenAiAuthInterceptor
     */
    @Getter
    private OpenAiAuthInterceptor authInterceptor;

    /**
     * 默认的分页参数
     */
    @Getter
    private PageRequest pageRequest = PageRequest.builder().build();

    @Getter
    private static final Headers assistantsHeader = Headers.of("OpenAI-Beta", "assistants=v1");

    /**
     * 构造器
     *
     * @return OpenAiClient.Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public DeepSeekClient() {
    }

    /**
     * 构造
     *
     * @param builder
     */
    private DeepSeekClient(Builder builder) {
        if (CollectionUtil.isEmpty(builder.apiKey)) {
            throw new BaseException(CommonError.API_KEYS_NOT_NUL);
        }
        apiKey = builder.apiKey;

        if (StrUtil.isBlank(builder.apiHost)) {
            builder.apiHost = DeepSeekConst.HOST;
        }
        apiHost = builder.apiHost;

        if (Objects.isNull(builder.keyStrategy)) {
            builder.keyStrategy = new KeyRandomStrategy();
        }
        keyStrategy = builder.keyStrategy;

        if (Objects.isNull(builder.authInterceptor)) {
            builder.authInterceptor = new DefaultOpenAiAuthInterceptor();
        }
        authInterceptor = builder.authInterceptor;
        authInterceptor.setApiKey(this.apiKey);
        authInterceptor.setKeyStrategy(this.keyStrategy);

        if (Objects.isNull(builder.okHttpClient)) {
            builder.okHttpClient = this.okHttpClient();
        } else {
            //自定义的okhttpClient  需要增加api keys
            builder.okHttpClient = builder.okHttpClient
                    .newBuilder()
                    .addInterceptor(authInterceptor)
                    .build();
        }
        okHttpClient = builder.okHttpClient;
        this.openAiApi = new Retrofit.Builder()
                .baseUrl(apiHost)
                .client(okHttpClient)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create())
                .build().create(OpenAiApi.class);
    }


    /**
     * 创建默认OkHttpClient
     *
     * @return
     */
    private OkHttpClient okHttpClient() {
        if (Objects.isNull(this.authInterceptor)) {
            this.authInterceptor = new DefaultOpenAiAuthInterceptor();
        }
        this.authInterceptor.setApiKey(this.apiKey);
        this.authInterceptor.setKeyStrategy(this.keyStrategy);
        return new OkHttpClient
                .Builder()
                .addInterceptor(this.authInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS).build();
    }

    /**
     * openAi模型列表
     *
     * @return Model  list
     */
    public List<Model> models() {
        Single<ModelResponse> models = this.openAiApi.models();
        return models.blockingGet().getData();
    }

    /**
     * openAi模型详细信息
     *
     * @param id 模型主键
     * @return Model    模型类
     */
    public Model model(String id) {
        if (Objects.isNull(id) || "".equals(id)) {
            throw new BaseException(CommonError.PARAM_ERROR);
        }
        Single<Model> model = this.openAiApi.model(id);
        return model.blockingGet();
    }


    /**
     * 问答接口
     *
     * @param completion 问答参数
     * @return CompletionResponse
     */
    public CompletionResponse completions(Completion completion) {
        Single<CompletionResponse> completions = this.openAiApi.completions(completion);
        return completions.blockingGet();
    }

    /**
     * 问答接口-简易版
     *
     * @param question 问题描述
     * @return CompletionResponse
     */
    public CompletionResponse completions(String question) {
        Completion q = Completion.builder()
                .prompt(question)
                .build();
        Single<CompletionResponse> completions = this.openAiApi.completions(q);
        return completions.blockingGet();
    }

    /**
     * 文本修改
     *
     * @param edit 图片对象
     * @return EditResponse
     */
    @Deprecated
    public EditResponse edit(Edit edit) {
        Single<EditResponse> edits = this.openAiApi.edits(edit);
        return edits.blockingGet();
    }

    /**
     * 根据描述生成图片
     *
     * @param prompt 描述信息
     * @return ImageResponse
     */
    public ImageResponse genImages(String prompt) {
        Image image = Image.builder().prompt(prompt).build();
        return this.genImages(image);
    }

    /**
     * 根据描述生成图片
     *
     * @param image 图片参数
     * @return ImageResponse
     */
    public ImageResponse genImages(Image image) {
        Single<ImageResponse> edits = this.openAiApi.genImages(image);
        return edits.blockingGet();
    }

    /**
     * Creates an edited or extended image given an original image and a prompt.
     * 根据描述修改图片
     *
     * @param image  图片对象
     * @param prompt 描述信息
     * @return Item  list
     */
    public List<Item> editImages(java.io.File image, String prompt) {
        ImageEdit imageEdit = ImageEdit.builder().prompt(prompt).build();
        return this.editImages(image, null, imageEdit);
    }

    /**
     * Creates an edited or extended image given an original image and a prompt.
     * 根据描述修改图片
     *
     * @param image     图片对象
     * @param imageEdit 图片参数
     * @return Item  list
     */
    public List<Item> editImages(java.io.File image, ImageEdit imageEdit) {
        return this.editImages(image, null, imageEdit);
    }

    /**
     * Creates an edited or extended image given an original image and a prompt.
     * 根据描述修改图片
     *
     * @param image     png格式的图片，最大4MB
     * @param mask      png格式的图片，最大4MB
     * @param imageEdit 图片参数
     * @return Item list
     */
    public List<Item> editImages(java.io.File image, java.io.File mask, ImageEdit imageEdit) {
        checkImage(image);
        checkImageFormat(image);
        checkImageSize(image);
        if (Objects.nonNull(mask)) {
            checkImageFormat(image);
            checkImageSize(image);
        }
        // 创建 RequestBody，用于封装构建RequestBody
        RequestBody imageBody = RequestBody.create(MediaType.parse("multipart/form-data"), image);
        MultipartBody.Part imageMultipartBody = MultipartBody.Part.createFormData("image", image.getName(), imageBody);
        MultipartBody.Part maskMultipartBody = null;
        if (Objects.nonNull(mask)) {
            RequestBody maskBody = RequestBody.create(MediaType.parse("multipart/form-data"), mask);
            maskMultipartBody = MultipartBody.Part.createFormData("mask", image.getName(), maskBody);
        }
        Map<String, RequestBody> requestBodyMap = new HashMap<>();
        requestBodyMap.put("prompt", RequestBody.create(MediaType.parse("multipart/form-data"), imageEdit.getPrompt()));
        requestBodyMap.put("n", RequestBody.create(MediaType.parse("multipart/form-data"), imageEdit.getN().toString()));
        requestBodyMap.put("size", RequestBody.create(MediaType.parse("multipart/form-data"), imageEdit.getSize()));
        requestBodyMap.put("response_format", RequestBody.create(MediaType.parse("multipart/form-data"), imageEdit.getResponseFormat()));
        if (!(Objects.isNull(imageEdit.getUser()) || "".equals(imageEdit.getUser()))) {
            requestBodyMap.put("user", RequestBody.create(MediaType.parse("multipart/form-data"), imageEdit.getUser()));
        }
        Single<ImageResponse> imageResponse = this.openAiApi.editImages(
                imageMultipartBody,
                maskMultipartBody,
                requestBodyMap
        );
        return imageResponse.blockingGet().getData();
    }

    /**
     * Creates a variation of a given image.
     * <p>
     * 变化图片，类似ai重做图片
     *
     * @param image           图片对象
     * @param imageVariations 图片参数
     * @return ImageResponse
     */
    public ImageResponse variationsImages(java.io.File image, ImageVariations imageVariations) {
        checkImage(image);
        checkImageFormat(image);
        checkImageSize(image);
        RequestBody imageBody = RequestBody.create(MediaType.parse("multipart/form-data"), image);
        MultipartBody.Part multipartBody = MultipartBody.Part.createFormData("image", image.getName(), imageBody);
        Map<String, RequestBody> requestBodyMap = new HashMap<>();
        requestBodyMap.put("n", RequestBody.create(MediaType.parse("multipart/form-data"), imageVariations.getN().toString()));
        requestBodyMap.put("size", RequestBody.create(MediaType.parse("multipart/form-data"), imageVariations.getSize()));
        requestBodyMap.put("response_format", RequestBody.create(MediaType.parse("multipart/form-data"), imageVariations.getResponseFormat()));
        if (!(Objects.isNull(imageVariations.getUser()) || "".equals(imageVariations.getUser()))) {
            requestBodyMap.put("user", RequestBody.create(MediaType.parse("multipart/form-data"), imageVariations.getUser()));
        }
        Single<ImageResponse> variationsImages = this.openAiApi.variationsImages(
                multipartBody,
                requestBodyMap
        );
        return variationsImages.blockingGet();
    }

    /**
     * Creates a variation of a given image.
     *
     * @param image 图片对象
     * @return ImageResponse
     */
    public ImageResponse variationsImages(java.io.File image) {
        checkImage(image);
        checkImageFormat(image);
        checkImageSize(image);
        ImageVariations imageVariations = ImageVariations.builder().build();
        return this.variationsImages(image, imageVariations);
    }

    /**
     * 校验图片不能为空
     *
     * @param image
     */
    private void checkImage(java.io.File image) {
        if (Objects.isNull(image)) {
            log.error("image不能为空");
            throw new BaseException(CommonError.PARAM_ERROR);
        }
    }

    /**
     * 校验图片格式
     *
     * @param image
     */
    private void checkImageFormat(java.io.File image) {
        if (!(image.getName().endsWith("png") || image.getName().endsWith("PNG"))) {
            log.error("image格式错误");
            throw new BaseException(CommonError.PARAM_ERROR);
        }
    }

    /**
     * 校验图片大小
     *
     * @param image
     */
    private void checkImageSize(java.io.File image) {
        if (image.length() > 4 * 1024 * 1024) {
            log.error("image最大支持4MB");
            throw new BaseException(CommonError.PARAM_ERROR);
        }
    }

    /**
     * 向量计算：单文本
     *
     * @param input 单文本
     * @return EmbeddingResponse
     */
    public EmbeddingResponse embeddings(String input) {
        List<String> inputs = new ArrayList<>(1);
        inputs.add(input);
        Embedding embedding = Embedding.builder().input(inputs).build();
        return this.embeddings(embedding);
    }

    /**
     * 向量计算：集合文本
     *
     * @param input 文本集合
     * @return EmbeddingResponse
     */
    public EmbeddingResponse embeddings(List<String> input) {
        Embedding embedding = Embedding.builder().input(input).build();
        return this.embeddings(embedding);
    }

    /**
     * 文本转换向量
     *
     * @param embedding 入参
     * @return EmbeddingResponse
     */
    public EmbeddingResponse embeddings(Embedding embedding) {
        Single<EmbeddingResponse> embeddings = this.openAiApi.embeddings(embedding);
        return embeddings.blockingGet();
    }

    /**
     * 获取文件列表
     *
     * @return File  list
     */
    public List<File> files() {
        Single<OpenAiResponse<File>> files = this.openAiApi.files();
        return files.blockingGet().getData();
    }

    /**
     * 删除文件
     *
     * @param fileId 文件id
     * @return DeleteResponse
     */
    public DeleteResponse deleteFile(String fileId) {
        Single<DeleteResponse> deleteFile = this.openAiApi.deleteFile(fileId);
        return deleteFile.blockingGet();
    }

    /**
     * 上传文件
     *
     * @param purpose purpose
     * @param file    文件对象
     * @return UploadFileResponse
     */
    public UploadFileResponse uploadFile(String purpose, java.io.File file) {
        // 创建 RequestBody，用于封装构建RequestBody
        RequestBody fileBody = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        MultipartBody.Part multipartBody = MultipartBody.Part.createFormData("file", file.getName(), fileBody);

        RequestBody purposeBody = RequestBody.create(MediaType.parse("multipart/form-data"), purpose);
        Single<UploadFileResponse> uploadFileResponse = this.openAiApi.uploadFile(multipartBody, purposeBody);
        return uploadFileResponse.blockingGet();
    }

    /**
     * 上传文件
     *
     * @param file 文件
     * @return UploadFileResponse
     */
    public UploadFileResponse uploadFile(java.io.File file) {
        //purpose 官网示例默认是：fine-tune
        return this.uploadFile("fine-tune", file);
    }

    /**
     * 检索文件
     *
     * @param fileId 文件id
     * @return File
     */
    public File retrieveFile(String fileId) {
        Single<File> fileContent = this.openAiApi.retrieveFile(fileId);
        return fileContent.blockingGet();
    }

    /**
     * 检索文件内容
     * 免费用户无法使用此接口 #未经过测试
     *
     * @param fileId
     * @return ResponseBody
     */
//    public ResponseBody retrieveFileContent(String fileId) {
//        Single<ResponseBody> fileContent = this.openAiApi.retrieveFileContent(fileId);
//        return fileContent.blockingGet();
//    }

    /**
     * 文本审核
     *
     * @param input 待检测数据
     * @return ModerationResponse
     */
    public ModerationResponse moderations(String input) {
        List<String> content = new ArrayList<>(1);
        content.add(input);
        Moderation moderation = Moderation.builder().input(content).build();
        return this.moderations(moderation);
    }

    /**
     * 文本审核
     *
     * @param input 待检测数据集合
     * @return ModerationResponse
     */
    public ModerationResponse moderations(List<String> input) {
        Moderation moderation = Moderation.builder().input(input).build();
        return this.moderations(moderation);
    }

    /**
     * 文本审核
     *
     * @param moderation 审核参数
     * @return ModerationResponse
     */
    public ModerationResponse moderations(Moderation moderation) {
        Single<ModerationResponse> moderations = this.openAiApi.moderations(moderation);
        return moderations.blockingGet();
    }

    /**
     * 创建微调模型
     *
     * @param fineTune 微调作业id
     * @return FineTuneResponse
     * @see #fineTuneJob(FineTuneJob fineTuneJob)
     */
    @Deprecated
    public FineTuneResponse fineTune(FineTune fineTune) {
        Single<FineTuneResponse> fineTuneResponse = this.openAiApi.fineTune(fineTune);
        return fineTuneResponse.blockingGet();
    }

    /**
     * 创建微调模型
     *
     * @param trainingFileId 文件id，文件上传返回的id
     * @return FineTuneResponse
     * @see #fineTuneJob(String trainingFileId)
     */
    @Deprecated
    public FineTuneResponse fineTune(String trainingFileId) {
        FineTune fineTune = FineTune.builder().trainingFile(trainingFileId).build();
        return this.fineTune(fineTune);
    }

    /**
     * 微调模型列表
     *
     * @return FineTuneResponse list
     * @see #fineTuneJobs(String, Integer)
     */
    @Deprecated
    public List<FineTuneResponse> fineTunes() {
        Single<OpenAiResponse<FineTuneResponse>> fineTunes = this.openAiApi.fineTunes();
        return fineTunes.blockingGet().getData();
    }

    /**
     * 检索微调作业
     *
     * @param fineTuneId 微调作业id
     * @return FineTuneResponse
     * @see #retrieveFineTuneJob(String fineTuneJobId)
     */
    @Deprecated
    public FineTuneResponse retrieveFineTune(String fineTuneId) {
        Single<FineTuneResponse> fineTune = this.openAiApi.retrieveFineTune(fineTuneId);
        return fineTune.blockingGet();
    }

    /**
     * 取消微调作业
     *
     * @param fineTuneId 主键
     * @return FineTuneResponse
     * @see #cancelFineTuneJob(String fineTuneJobId)
     */
    @Deprecated
    public FineTuneResponse cancelFineTune(String fineTuneId) {
        Single<FineTuneResponse> fineTune = this.openAiApi.cancelFineTune(fineTuneId);
        return fineTune.blockingGet();
    }

    /**
     * 微调作业事件列表
     *
     * @param fineTuneId 微调作业id
     * @return Event List
     * @see #fineTuneJobEvents(String, String, Integer)
     */
    @Deprecated
    public List<Event> fineTuneEvents(String fineTuneId) {
        Single<OpenAiResponse<Event>> events = this.openAiApi.fineTuneEvents(fineTuneId);
        return events.blockingGet().getData();
    }

    /**
     * 删除微调作业模型
     * Delete a fine-tuned model. You must have the Owner role in your organization.
     *
     * @param model 模型名称
     * @return FineTuneDeleteResponse
     */
    public FineTuneDeleteResponse deleteFineTuneModel(String model) {
        Single<FineTuneDeleteResponse> delete = this.openAiApi.deleteFineTuneModel(model);
        return delete.blockingGet();
    }


    /**
     * 引擎列表
     *
     * @return Engine List
     */
    @Deprecated
    public List<Engine> engines() {
        Single<OpenAiResponse<Engine>> engines = this.openAiApi.engines();
        return engines.blockingGet().getData();
    }

    /**
     * 引擎详细信息
     *
     * @param engineId 引擎id
     * @return Engine
     */
    @Deprecated
    public Engine engine(String engineId) {
        Single<Engine> engine = this.openAiApi.engine(engineId);
        return engine.blockingGet();
    }

    /**
     * 最新版的GPT-3.5 chat completion 更加贴近官方网站的问答模型
     *
     * @param chatCompletion 问答参数
     * @return 答案
     */
    public <T extends BaseChatCompletion> ChatCompletionResponse chatCompletion(T chatCompletion) {
        if (chatCompletion instanceof ChatCompletion) {
            Single<ChatCompletionResponse> chatCompletionResponse = this.openAiApi.chatCompletion((ChatCompletion) chatCompletion);
            return chatCompletionResponse.blockingGet();
        }
        Single<ChatCompletionResponse> chatCompletionResponse = this.openAiApi.chatCompletionWithPicture((ChatCompletionWithPicture) chatCompletion);
        return chatCompletionResponse.blockingGet();
    }

    /**
     * 简易版（不支持图片输入）
     *
     * @param messages 问答参数
     * @return 答案
     */
    public ChatCompletionResponse chatCompletion(List<Message> messages) {
        ChatCompletion chatCompletion = ChatCompletion.builder().messages(messages).build();
        return this.chatCompletion(chatCompletion);
    }

    /**
     * 插件问答简易版
     * 默认取messages最后一个元素构建插件对话
     * 默认模型：ChatCompletion.Model.GPT_3_5_TURBO_16K_0613
     *
     * @param chatCompletion 参数
     * @param plugin         插件
     * @param <R>            插件自定义函数的请求值
     * @param <T>            插件自定义函数的返回值
     * @return ChatCompletionResponse
     */
    public <R extends PluginParam, T> ChatCompletionResponse chatCompletionWithPlugin(ChatCompletion chatCompletion, PluginAbstract<R, T> plugin) {
        if (Objects.isNull(plugin)) {
            return this.chatCompletion(chatCompletion);
        }
        if (CollectionUtil.isEmpty(chatCompletion.getMessages())) {
            throw new BaseException(CommonError.MESSAGE_NOT_NUL);
        }
        List<Message> messages = chatCompletion.getMessages();
        Functions functions = Functions.builder()
                .name(plugin.getFunction())
                .description(plugin.getDescription())
                .parameters(plugin.getParameters())
                .build();
        //没有值，设置默认值
        if (Objects.isNull(chatCompletion.getFunctionCall())) {
            chatCompletion.setFunctionCall("auto");
        }
        //tip: 覆盖自己设置的functions参数，使用plugin构造的functions
        chatCompletion.setFunctions(Collections.singletonList(functions));
        //调用OpenAi
        ChatCompletionResponse functionCallChatCompletionResponse = this.chatCompletion(chatCompletion);
        ChatChoice chatChoice = functionCallChatCompletionResponse.getChoices().get(0);
        log.debug("构造的方法值：{}", chatChoice.getMessage().getFunctionCall());

        R realFunctionParam = (R) JSONUtil.toBean(chatChoice.getMessage().getFunctionCall().getArguments(), plugin.getR());
        T tq = plugin.func(realFunctionParam);

        FunctionCall functionCall = FunctionCall.builder()
                .arguments(chatChoice.getMessage().getFunctionCall().getArguments())
                .name(plugin.getFunction())
                .build();
        messages.add(Message.builder().role(Message.Role.ASSISTANT).content("function_call").functionCall(functionCall).build());
        messages.add(Message.builder().role(Message.Role.FUNCTION).name(plugin.getFunction()).content(plugin.content(tq)).build());
        //设置第二次，请求的参数
        chatCompletion.setFunctionCall(null);
        chatCompletion.setFunctions(null);

        ChatCompletionResponse chatCompletionResponse = this.chatCompletion(chatCompletion);
        log.debug("自定义的方法返回值：{}", chatCompletionResponse.getChoices());
        return chatCompletionResponse;
    }

    /**
     * 插件问答简易版
     * 默认取messages最后一个元素构建插件对话
     * 默认模型：ChatCompletion.Model.GPT_3_5_TURBO_16K_0613
     *
     * @param messages 问答参数
     * @param plugin   插件
     * @param <R>      插件自定义函数的请求值
     * @param <T>      插件自定义函数的返回值
     * @return ChatCompletionResponse
     */
    public <R extends PluginParam, T> ChatCompletionResponse chatCompletionWithPlugin(List<Message> messages, PluginAbstract<R, T> plugin) {
        return chatCompletionWithPlugin(messages, ChatCompletion.Model.GPT_3_5_TURBO_16K_0613.getName(), plugin);
    }


    /**
     * 插件问答简易版
     * 默认取messages最后一个元素构建插件对话
     *
     * @param messages 问答参数
     * @param model    模型
     * @param plugin   插件
     * @param <R>      插件自定义函数的请求值
     * @param <T>      插件自定义函数的返回值
     * @return ChatCompletionResponse
     */
    public <R extends PluginParam, T> ChatCompletionResponse chatCompletionWithPlugin(List<Message> messages, String model, PluginAbstract<R, T> plugin) {
        ChatCompletion chatCompletion = ChatCompletion.builder().messages(messages).model(model).build();
        return this.chatCompletionWithPlugin(chatCompletion, plugin);
    }


    /**
     * 语音转文字
     *
     * @param transcriptions 参数
     * @param file           语音文件 最大支持25MB mp3, mp4, mpeg, mpga, m4a, wav, webm
     * @return 语音文本
     */
    public WhisperResponse speechToTextTranscriptions(java.io.File file, Transcriptions transcriptions) {
        //文件
        RequestBody fileBody = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        MultipartBody.Part multipartBody = MultipartBody.Part.createFormData("file", file.getName(), fileBody);
        //自定义参数
        Map<String, RequestBody> requestBodyMap = new HashMap<>();
        if (StrUtil.isNotBlank(transcriptions.getLanguage())) {
            requestBodyMap.put(Transcriptions.Fields.language, RequestBody.create(MediaType.parse("multipart/form-data"), transcriptions.getLanguage()));
        }
        if (StrUtil.isNotBlank(transcriptions.getModel())) {
            requestBodyMap.put(Transcriptions.Fields.model, RequestBody.create(MediaType.parse("multipart/form-data"), transcriptions.getModel()));
        }
        if (StrUtil.isNotBlank(transcriptions.getPrompt())) {
            requestBodyMap.put(Transcriptions.Fields.prompt, RequestBody.create(MediaType.parse("multipart/form-data"), transcriptions.getPrompt()));
        }
        if (StrUtil.isNotBlank(transcriptions.getResponseFormat())) {
            requestBodyMap.put(Transcriptions.Fields.responseFormat, RequestBody.create(MediaType.parse("multipart/form-data"), transcriptions.getResponseFormat()));
        }
        if (Objects.nonNull(transcriptions.getTemperature())) {
            requestBodyMap.put(Transcriptions.Fields.temperature, RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(transcriptions.getTemperature())));
        }
        Single<WhisperResponse> whisperResponse = this.openAiApi.speechToTextTranscriptions(multipartBody, requestBodyMap);
        return whisperResponse.blockingGet();
    }

    /**
     * 简易版 语音转文字
     *
     * @param file 语音文件 最大支持25MB mp3, mp4, mpeg, mpga, m4a, wav, webm
     * @return 语音文本
     */
    public WhisperResponse speechToTextTranscriptions(java.io.File file) {
        Transcriptions transcriptions = Transcriptions.builder().build();
        return this.speechToTextTranscriptions(file, transcriptions);

    }


    /**
     * 语音翻译：目前仅支持翻译为英文
     *
     * @param translations 参数
     * @param file         语音文件 最大支持25MB mp3, mp4, mpeg, mpga, m4a, wav, webm
     * @return 翻译后文本
     */
    public WhisperResponse speechToTextTranslations(java.io.File file, Translations translations) {
        //文件
        RequestBody fileBody = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        MultipartBody.Part multipartBody = MultipartBody.Part.createFormData("file", file.getName(), fileBody);
        //自定义参数
        Map<String, RequestBody> requestBodyMap = new HashMap<>(5, 1L);

        if (StrUtil.isNotBlank(translations.getModel())) {
            requestBodyMap.put(Translations.Fields.model, RequestBody.create(MediaType.parse("multipart/form-data"), translations.getModel()));
        }
        if (StrUtil.isNotBlank(translations.getPrompt())) {
            requestBodyMap.put(Translations.Fields.prompt, RequestBody.create(MediaType.parse("multipart/form-data"), translations.getPrompt()));
        }
        if (StrUtil.isNotBlank(translations.getResponseFormat())) {
            requestBodyMap.put(Translations.Fields.responseFormat, RequestBody.create(MediaType.parse("multipart/form-data"), translations.getResponseFormat()));
        }
        requestBodyMap.put(Translations.Fields.temperature, RequestBody.create(MediaType.parse("multipart/form-data"), String.valueOf(translations.getTemperature())));
        Single<WhisperResponse> whisperResponse = this.openAiApi.speechToTextTranslations(multipartBody, requestBodyMap);
        return whisperResponse.blockingGet();
    }

    /**
     * 简易版 语音翻译：目前仅支持翻译为英文
     *
     * @param file 语音文件 最大支持25MB mp3, mp4, mpeg, mpga, m4a, wav, webm
     * @return 翻译后文本
     */
    public WhisperResponse speechToTextTranslations(java.io.File file) {
        Translations translations = Translations.builder().build();
        return this.speechToTextTranslations(file, translations);
    }

    /**
     * 校验语音文件大小给出提示，目前官方限制25MB，后续可能会改动所以不报错只做提示
     *
     * @param file
     */
    private void checkSpeechFileSize(java.io.File file) {
        if (file.length() > 25 * 1204 * 1024) {
            log.warn("2023-03-02官方文档提示：文件不能超出25MB");
        }
    }

    /**
     * ## 官方已经禁止使用此api
     * OpenAi账户余额查询
     *
     * @return 余额
     * @see #subscription()
     * @see #billingUsage(LocalDate, LocalDate)
     */
    @Deprecated
    public CreditGrantsResponse creditGrants() {
        Single<CreditGrantsResponse> creditGrants = this.openAiApi.creditGrants();
        return creditGrants.blockingGet();
    }

    /**
     * 账户信息查询：里面包含总金额等信息
     *
     * @return 账户信息
     */
    public Subscription subscription() {
        Single<Subscription> subscription = this.openAiApi.subscription();
        return subscription.blockingGet();
    }

    /**
     * 账户调用接口消耗金额信息查询
     * 最多查询100天
     *
     * @param starDate 开始时间
     * @param endDate  结束时间
     * @return 消耗金额信息
     */
    public BillingUsage billingUsage(@NotNull LocalDate starDate, @NotNull LocalDate endDate) {
        Single<BillingUsage> billingUsage = this.openAiApi.billingUsage(starDate, endDate);
        return billingUsage.blockingGet();
    }

    /**
     * 创建微调job
     *
     * @param fineTuneJob 微调job
     * @return FineTuneJobResponse
     * @since 1.1.2
     */
    public FineTuneJobResponse fineTuneJob(FineTuneJob fineTuneJob) {
        Single<FineTuneJobResponse> fineTuneJobResponse = this.openAiApi.fineTuneJob(fineTuneJob);
        return fineTuneJobResponse.blockingGet();
    }

    /**
     * 创建微调job
     *
     * @param trainingFileId 文件id，文件上传返回的id
     * @return FineTuneJobResponse
     * @since 1.1.2
     */
    public FineTuneJobResponse fineTuneJob(String trainingFileId) {
        FineTuneJob fineTuneJob = FineTuneJob.builder()
                .model(FineTuneJob.Model.GPT_3_5_TURBO_1106.getName())
                .trainingFile(trainingFileId)
                .build();
        return this.fineTuneJob(fineTuneJob);
    }

    /**
     * 微调job集合
     *
     * @param after 上一个分页请求中最后一个job id，默认值：null
     * @param limit 每次查询数量 默认值：20
     * @return FineTuneJobListResponse #FineTuneResponse
     * @since 1.1.2
     */
    public FineTuneJobListResponse<FineTuneJobResponse> fineTuneJobs(String after, Integer limit) {
        Single<FineTuneJobListResponse<FineTuneJobResponse>> fineTuneJobs = this.openAiApi.fineTuneJobs(after, limit);
        return fineTuneJobs.blockingGet();
    }

    /**
     * 检索微调job
     *
     * @param fineTuneJobId 微调job id
     * @return FineTuneResponse
     * @since 1.1.2
     */
    public FineTuneJobResponse retrieveFineTuneJob(String fineTuneJobId) {
        Single<FineTuneJobResponse> fineTuneJob = this.openAiApi.retrieveFineTuneJob(fineTuneJobId);
        return fineTuneJob.blockingGet();
    }

    /**
     * 取消微调job
     *
     * @param fineTuneJobId 微调job id
     * @return FineTuneJobResponse
     * @since 1.1.2
     */
    public FineTuneJobResponse cancelFineTuneJob(String fineTuneJobId) {
        Single<FineTuneJobResponse> fineTuneJob = this.openAiApi.cancelFineTuneJob(fineTuneJobId);
        return fineTuneJob.blockingGet();
    }

    /**
     * 微调作业事件列表
     *
     * @param fineTuneJobId 微调job id
     * @param after         上一个分页请求中最后一个id，默认值：null
     * @param limit         每次查询数量 默认值：20
     * @return Event List
     * @since 1.1.2
     */
    public FineTuneJobListResponse<FineTuneJobEvent> fineTuneJobEvents(String fineTuneJobId, String after, Integer limit) {
        Single<FineTuneJobListResponse<FineTuneJobEvent>> events = this.openAiApi.fineTuneJobEvents(fineTuneJobId, after, limit);
        return events.blockingGet();
    }

    /**
     * 文本转语音（异步）
     *
     * @param textToSpeech 参数
     * @param callback     返回值接收
     * @since 1.1.2
     */
    public void textToSpeech(TextToSpeech textToSpeech, Callback callback) {
        Call<ResponseBody> responseBody = this.openAiApi.textToSpeech(textToSpeech);
        responseBody.enqueue(callback);
    }

    /**
     * 文本转语音（同步）
     *
     * @param textToSpeech 参数
     * @since 1.1.3
     */
    public ResponseBody textToSpeech(TextToSpeech textToSpeech) throws IOException {
        Call<ResponseBody> responseBody = this.openAiApi.textToSpeech(textToSpeech);
        return responseBody.execute().body();
    }

    /**
     * 创建助手
     *
     * @param assistant 参数
     * @return 返回助手信息
     * @since 1.1.2
     */
    public AssistantResponse assistant(Assistant assistant) {
        Single<AssistantResponse> assistantResponse = this.openAiApi.assistant(assistant);
        return assistantResponse.blockingGet();
    }

    /**
     * 获取助手详细信息
     *
     * @param assistantId 助手id
     * @return 助手信息
     * @since 1.1.3
     */
    public AssistantResponse retrieveAssistant(String assistantId) {
        Single<AssistantResponse> assistant = this.openAiApi.retrieveAssistant(assistantId);
        return assistant.blockingGet();
    }


    /**
     * 修改助手信息
     *
     * @param assistantId 助手id
     * @param assistant   修改助手参数
     * @return 助手信息
     * @since 1.1.3
     */
    public AssistantResponse modifyAssistant(String assistantId, Assistant assistant) {
        Single<AssistantResponse> assistantResponse = this.openAiApi.modifyAssistant(assistantId, assistant);
        return assistantResponse.blockingGet();
    }

    /**
     * 删除助手
     *
     * @param assistantId 助手id
     * @return 删除状态
     * @since 1.1.3
     */
    public DeleteResponse deleteAssistant(String assistantId) {
        Single<DeleteResponse> deleteAssistant = this.openAiApi.deleteAssistant(assistantId);
        return deleteAssistant.blockingGet();
    }

    /**
     * 助手列表
     *
     * @param pageRequest 分页信息
     * @return AssistantListResponse #AssistantResponse
     * @since 1.1.3
     */
    public AssistantListResponse<AssistantResponse> assistants(PageRequest pageRequest) {
        Single<AssistantListResponse<AssistantResponse>> assistants = this.openAiApi.assistants(pageRequest.getLimit(), pageRequest.getOrder(), pageRequest.getBefore(), pageRequest.getAfter());
        return assistants.blockingGet();
    }

    /**
     * 创建助手文件
     *
     * @param assistantId 助手id
     * @param assistantId 文件信息
     * @return 返回信息AssistantResponse
     */
    public AssistantFileResponse assistantFile(String assistantId, AssistantFile assistantFile) {
        Single<AssistantFileResponse> assistantFileResponse = this.openAiApi.assistantFile(assistantId, assistantFile);
        return assistantFileResponse.blockingGet();
    }

    /**
     * 检索助手文件
     *
     * @param assistantId 助手id
     * @param fileId      文件信息
     * @return 助手文件信息
     */
    public AssistantFileResponse retrieveAssistantFile(String assistantId, String fileId) {
        Single<AssistantFileResponse> assistantFileResponse = this.openAiApi.retrieveAssistantFile(assistantId, fileId);
        return assistantFileResponse.blockingGet();
    }

    /**
     * 删除助手文件
     *
     * @param assistantId 助手id
     * @param fileId      文件信息
     * @return 删除状态
     */
    public DeleteResponse deleteAssistantFile(String assistantId, String fileId) {
        Single<DeleteResponse> deleteResponse = this.openAiApi.deleteAssistantFile(assistantId, fileId);
        return deleteResponse.blockingGet();
    }

    /**
     * 助手文件列表
     *
     * @param assistantId 助手id
     * @param pageRequest 分页信息
     * @return 助手文件列表
     */
    public AssistantListResponse<AssistantFileResponse> assistantFiles(String assistantId, PageRequest pageRequest) {
        pageRequest = Optional.ofNullable(pageRequest).orElse(this.getPageRequest());
        Single<AssistantListResponse<AssistantFileResponse>> deleteResponse = this.openAiApi.assistantFiles(assistantId, pageRequest.getLimit(), pageRequest.getOrder(), pageRequest.getBefore(), pageRequest.getAfter());
        return deleteResponse.blockingGet();
    }

    /**
     * 创建线程
     *
     * @param thread 创建线程参数
     * @return 线程信息
     * @since 1.1.3
     */
    public ThreadResponse thread(Thread thread) {
        return this.openAiApi.thread(thread).blockingGet();
    }

    /**
     * 获取线程详细信息
     *
     * @param threadId 线程id
     * @return 线程信息
     * @since 1.1.3
     */
    public ThreadResponse retrieveThread(String threadId) {
        return this.openAiApi.retrieveThread(threadId).blockingGet();
    }

    /**
     * 修改线程信息
     *
     * @param threadId 线程id
     * @param thread   线程信息
     * @return 线程信息
     * @since 1.1.3
     */
    public ThreadResponse modifyThread(String threadId, ModifyThread thread) {
        return this.openAiApi.modifyThread(threadId, thread).blockingGet();
    }

    /**
     * 删除线程
     *
     * @param threadId 线程id
     * @return 删除状态
     * @since 1.1.3
     */
    public DeleteResponse deleteThread(String threadId) {
        return this.openAiApi.deleteThread(threadId).blockingGet();
    }

    /**
     * 为线程创建消息
     *
     * @param threadId 线程id
     * @param message  message参数
     * @return
     * @since 1.1.3
     */
    public MessageResponse message(String threadId, ThreadMessage message) {
        return this.openAiApi.message(threadId, message).blockingGet();
    }

    /**
     * 检索某一个线程对应的消息详细信息
     *
     * @param threadId  线程id
     * @param messageId 消息id
     * @return
     * @since 1.1.3
     */
    public MessageResponse retrieveMessage(String threadId, String messageId) {
        return this.openAiApi.retrieveMessage(threadId, messageId).blockingGet();
    }

    /**
     * 修改某一个线程对应的消息
     *
     * @param threadId  线程id
     * @param messageId 消息id
     * @param message   消息体
     * @return
     * @since 1.1.3
     */
    public MessageResponse modifyMessage(String threadId, String messageId, ModifyMessage message) {
        return this.openAiApi.modifyMessage(threadId, messageId, message).blockingGet();
    }

    /**
     * 获取某一个线程的消息集合
     *
     * @param threadId    线程id
     * @param pageRequest 分页信息
     * @return
     * @since 1.1.3
     */
    public AssistantListResponse<MessageResponse> messages(String threadId, PageRequest pageRequest) {
        pageRequest = Optional.ofNullable(pageRequest).orElse(this.getPageRequest());
        return this.openAiApi.messages(threadId,
                pageRequest.getLimit(),
                pageRequest.getOrder(),
                pageRequest.getBefore(),
                pageRequest.getAfter()).blockingGet();
    }

    /**
     * 检索某一个线程对应某一个消息的一个文件信息
     *
     * @param threadId  线程id
     * @param messageId 消息id
     * @param fileId    文件id
     * @return
     * @since 1.1.3
     */
    public MessageFileResponse retrieveMessageFile(String threadId, String messageId, String fileId) {
        return this.openAiApi.retrieveMessageFile(threadId, messageId, fileId).blockingGet();
    }

    /**
     * messageFiles集合
     *
     * @param threadId    线程id
     * @param messageId   消息id
     * @param pageRequest 分页信息
     * @return
     * @since 1.1.3
     */
    public AssistantListResponse<MessageFileResponse> messageFiles(String threadId, String messageId, PageRequest pageRequest) {
        pageRequest = Optional.ofNullable(pageRequest).orElse(this.getPageRequest());
        return this.openAiApi.messageFiles(threadId, messageId,
                pageRequest.getLimit(),
                pageRequest.getOrder(),
                pageRequest.getBefore(),
                pageRequest.getAfter()).blockingGet();
    }


    /**
     * 创建Run
     *
     * @param threadId 线程id
     * @param run      run
     * @return
     * @since 1.1.3
     */
    public RunResponse run(String threadId, Run run) {
        return this.openAiApi.run(threadId, run).blockingGet();
    }

    /**
     * SSE创建Run
     *
     * @param threadId            线程id
     * @param run                 run
     * @param eventSourceListener eventSourceListener
     * @return
     * @since 1.1.6
     */
    public void runWithStream(String threadId, Run run, EventSourceListener eventSourceListener) {
        if (!run.isStream()) {
            run.setStream(true);
        }
        SSEUtil.post(this.okHttpClient,
                this.apiHost + "v1/threads/" + threadId + "/runs",
                assistantsHeader,
                run,
                eventSourceListener
        );
    }


    /**
     * 检索run详细信息
     *
     * @param threadId 线程id
     * @param runId    run_id
     * @return
     * @since 1.1.3
     */
    public RunResponse retrieveRun(String threadId, String runId) {
        return this.openAiApi.retrieveRun(threadId, runId).blockingGet();
    }

    /**
     * 修改某一个run
     *
     * @param threadId 线程id
     * @param runId    run_id
     * @param run      消息体
     * @return
     * @since 1.1.3
     */
    public RunResponse modifyRun(String threadId, String runId, ModifyRun run) {
        return this.openAiApi.modifyRun(threadId, runId, run).blockingGet();
    }


    /**
     * 获取某一个线程的run集合
     *
     * @param threadId    线程id
     * @param pageRequest 分页信息
     * @return
     * @since 1.1.3
     */
    public AssistantListResponse<RunResponse> runs(String threadId, PageRequest pageRequest) {
        pageRequest = Optional.ofNullable(pageRequest).orElse(this.getPageRequest());
        return this.openAiApi.runs(threadId,
                pageRequest.getLimit(),
                pageRequest.getOrder(),
                pageRequest.getBefore(),
                pageRequest.getAfter()).blockingGet();
    }


    /**
     * 获取某一个线程的run集合
     *
     * @param threadId    线程id
     * @param runId       run id
     * @param toolOutputs 为其提交输出的工具列表。
     * @return
     * @since 1.1.3
     */
    public RunResponse submitToolOutputs(String threadId, String runId, ToolOutputBody toolOutputs) {
        return this.openAiApi.submitToolOutputs(threadId, runId, toolOutputs).blockingGet();
    }


    /**
     * 取消正在进行中的run
     *
     * @param threadId 线程id
     * @param runId    run id
     * @return
     * @since 1.1.3
     */
    public RunResponse cancelRun(String threadId, String runId) {
        return this.openAiApi.cancelRun(threadId, runId).blockingGet();
    }


    /**
     * 创建一个线程并在一个请求中运行它
     *
     * @param threadRun 对象
     * @return
     * @since 1.1.3
     */
    public RunResponse threadRun(ThreadRun threadRun) {
        return this.openAiApi.threadRun(threadRun).blockingGet();
    }

    /**
     * 检索run step详细信息
     *
     * @param threadId 线程id
     * @param runId    run_id
     * @param stepId   step_id
     * @return
     * @since 1.1.3
     */
    public RunStepResponse retrieveRunStep(String threadId, String runId, String stepId) {
        if (StrUtil.isBlank(stepId)) {
            log.error("step id不能为空");
            throw new BaseException(CommonError.PARAM_ERROR);
        }
        return this.openAiApi.retrieveRunStep(threadId, runId, stepId).blockingGet();
    }


    /**
     * 获取某一个线程的run step集合
     *
     * @param threadId    线程id
     * @param runId       run_id
     * @param pageRequest 分页信息
     * @return
     * @since 1.1.3
     */
    public AssistantListResponse<RunStepResponse> runSteps(String threadId, String runId, PageRequest pageRequest) {
        pageRequest = Optional.ofNullable(pageRequest).orElse(this.getPageRequest());
        return this.openAiApi.runSteps(threadId, runId,
                pageRequest.getLimit(),
                pageRequest.getOrder(),
                pageRequest.getBefore(),
                pageRequest.getAfter()).blockingGet();
    }


    public static final class Builder {
        /**
         * api keys
         */
        private @NotNull List<String> apiKey;
        /**
         * api请求地址，结尾处有斜杠
         *
         * @see DeepSeekConst
         */
        private String apiHost;
        /**
         * 自定义OkhttpClient
         */
        private OkHttpClient okHttpClient;

        /**
         * api key的获取策略
         */
        private KeyStrategyFunction keyStrategy;

        /**
         * 自定义鉴权拦截器
         */
        private OpenAiAuthInterceptor authInterceptor;

        public Builder() {
        }

        /**
         * @param val api请求地址，结尾处有斜杠
         * @return Builder对象
         * @see DeepSeekConst
         */
        public Builder apiHost(String val) {
            apiHost = val;
            return this;
        }

        public Builder apiKey(@NotNull List<String> val) {
            apiKey = val;
            return this;
        }

        public Builder keyStrategy(KeyStrategyFunction val) {
            keyStrategy = val;
            return this;
        }

        public Builder okHttpClient(OkHttpClient val) {
            okHttpClient = val;
            return this;
        }

        public Builder authInterceptor(OpenAiAuthInterceptor val) {
            authInterceptor = val;
            return this;
        }

        public DeepSeekClient build() {
            return new DeepSeekClient(this);
        }
    }
}
