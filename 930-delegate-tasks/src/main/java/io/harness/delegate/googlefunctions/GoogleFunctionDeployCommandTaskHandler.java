package io.harness.delegate.googlefunctions;

import com.google.cloud.functions.v2.CreateFunctionRequest;
import com.google.cloud.functions.v2.Function;
import com.google.inject.Inject;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.exception.GoogleFunctionException;
import io.harness.delegate.task.googlefunction.GoogleFunctionCommandTaskHelper;
import io.harness.delegate.task.googlefunction.GoogleFunctionUtils;
import io.harness.delegate.task.googlefunctions.GcpGoogleFunctionInfraConfig;
import io.harness.delegate.task.googlefunctions.GoogleFunction;
import io.harness.delegate.task.googlefunctions.GoogleFunctionInfraConfig;
import io.harness.delegate.task.googlefunctions.request.GoogleFunctionCommandRequest;
import io.harness.delegate.task.googlefunctions.request.GoogleFunctionDeployRequest;
import io.harness.delegate.task.googlefunctions.response.GoogleFunctionCommandResponse;
import io.harness.delegate.task.googlefunctions.response.GoogleFunctionDeployResponse;
import io.harness.exception.InvalidArgumentsException;
import io.harness.logging.CommandExecutionStatus;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@OwnedBy(HarnessTeam.CDP)
@NoArgsConstructor
@Slf4j
public class GoogleFunctionDeployCommandTaskHandler extends GoogleFunctionCommandTaskHandler {
    @Inject private GoogleFunctionCommandTaskHelper googleFunctionCommandTaskHelper;
    @Override
    protected GoogleFunctionCommandResponse executeTaskInternal(GoogleFunctionCommandRequest googleFunctionCommandRequest,
                                                                ILogStreamingTaskClient iLogStreamingTaskClient,
                                                                CommandUnitsProgress commandUnitsProgress) throws Exception {
        if (!(googleFunctionCommandRequest instanceof GoogleFunctionDeployRequest)) {
            throw new InvalidArgumentsException(Pair.of("googleFunctionCommandRequest", "Must be instance of " +
                    "GoogleFunctionCommandRequest"));
        }
        GoogleFunctionDeployRequest googleFunctionDeployRequest = (GoogleFunctionDeployRequest)
                googleFunctionCommandRequest;

        GcpGoogleFunctionInfraConfig googleFunctionInfraConfig = (GcpGoogleFunctionInfraConfig)
                googleFunctionDeployRequest.getGoogleFunctionInfraConfig();

        try{
            Function function = googleFunctionCommandTaskHelper.deployFunction(googleFunctionInfraConfig,
                    googleFunctionDeployRequest.getGoogleFunctionDeployManifestContent(),
                    googleFunctionDeployRequest.getUpdateFieldMaskContent(),
                    googleFunctionDeployRequest.getGoogleFunctionArtifactConfig(), true);

            GoogleFunction googleFunction = googleFunctionCommandTaskHelper.getGoogleFunction(function,
                    googleFunctionInfraConfig);

            return GoogleFunctionDeployResponse.builder()
                    .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                    .function(googleFunction)
                    .build();
        }
        catch(Exception exception) {
            throw new GoogleFunctionException(exception);
        }

    }
}
