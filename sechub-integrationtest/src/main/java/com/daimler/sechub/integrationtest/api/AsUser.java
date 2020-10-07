// SPDX-License-Identifier: MIT
package com.daimler.sechub.integrationtest.api;

import static com.daimler.sechub.integrationtest.api.TestAPI.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.daimler.sechub.commons.model.JSONConverter;
import com.daimler.sechub.integrationtest.JSONTestSupport;
import com.daimler.sechub.integrationtest.internal.IntegrationTestContext;
import com.daimler.sechub.integrationtest.internal.IntegrationTestFileSupport;
import com.daimler.sechub.integrationtest.internal.SecHubClientExecutor.ExecutionResult;
import com.daimler.sechub.integrationtest.internal.TestJSONHelper;
import com.daimler.sechub.integrationtest.internal.TestRestHelper;
import com.daimler.sechub.sharedkernel.mapping.MappingData;
import com.daimler.sechub.test.TestURLBuilder;
import com.daimler.sechub.test.TestUtil;
import com.daimler.sechub.test.executionprofile.TestExecutionProfile;
import com.daimler.sechub.test.executionprofile.TestExecutionProfileList;
import com.daimler.sechub.test.executorconfig.TestExecutorConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class AsUser {

    private static final Logger LOG = LoggerFactory.getLogger(AsUser.class);
    private JSONTestSupport jsonTestSupport = JSONTestSupport.DEFAULT;
    TestUser user;

    AsUser(TestUser user) {
        this.user = user;
    }

    public WithSecHubClient withSecHubClient() {
        return new WithSecHubClient(this);
    }

    /**
     * Upload given file
     *
     * @param userWantingToSignup
     * @return
     */
    public AsUser upload(TestProject project, UUID jobUUID, File file, String checkSum) {
        /* @formatter:off */
		getRestHelper().upload(getUrlBuilder().
		    		buildUploadSourceCodeUrl(project.getProjectId(),jobUUID),file,checkSum);
		/* @formatter:on */
        return this;
    }

    /**
     * Upload given resource, checksum will be automatically calculated
     *
     * @param userWantingToSignup
     * @return
     */
    public AsUser upload(TestProject project, UUID jobUUID, String pathInsideResources) {
        File uploadFile = IntegrationTestFileSupport.getTestfileSupport().createFileFromResourcePath(pathInsideResources);
        String checkSum = TestAPI.createSHA256Of(uploadFile);
        upload(project, jobUUID, uploadFile, checkSum);
        return this;
    }

    /**
     * Accept the user wanting to signup
     *
     * @param userWantingToSignup
     * @return
     */
    public AsUser acceptSignup(TestUser userWantingToSignup) {
        if (userWantingToSignup == null) {
            fail("user may not be null!");
            return null;
        }

        /* @formatter:off */
		getRestHelper().post(getUrlBuilder().
		    		buildAdminAcceptsUserSignUpUrl(userWantingToSignup.getUserId()));
		/* @formatter:on */
        return this;
    }

    private TestRestHelper getRestHelper() {
        return getContext().getRestHelper(user);
    }

    /**
     * Signup given (new) user
     *
     * @param user
     * @return this
     */
    public AsUser signUpAs(TestUser user) {

        String json = "{\"apiVersion\":\"1.0\",\r\n" + "		\"userId\":\"" + user.getUserId() + "\",\r\n" + "		\"emailAdress\":\"" + user.getEmail()
                + "\"}";
        getRestHelper().postJSon(getUrlBuilder().buildUserSignUpUrl(), json);
        return this;

    }

    public AsUser requestNewApiTokenFor(String emailAddress) {
        getRestHelper().postJSon(getUrlBuilder().buildAnonymousRequestNewApiToken(emailAddress), "");
        return this;
    }

    private TestURLBuilder getUrlBuilder() {
        return getContext().getUrlBuilder();
    }

    private IntegrationTestContext getContext() {
        return IntegrationTestContext.get();
    }

    /**
     * Tries to create the project
     *
     * @param project
     * @throws RestClientException
     */
    public void createProject(TestProject project, String ownerName) {
        if (ownerName == null) {
            // we use always the user how creates the project as owner when not explicit set
            ownerName = this.user.getUserId();
        }
        /* @formatter:off */
		StringBuilder json = new StringBuilder();
		TestJSONHelper jsonHelper = TestJSONHelper.get();
		json.append("{\n" +
				" \"apiVersion\":\"1.0\",\n" +
				" \"name\":\""+project.getProjectId()+"\",\n" +
				" \"owner\":\""+ownerName+"\",\n" +
				" \"description\":\""+project.getDescription()+"\"");
		if (! project.getWhiteListUrls().isEmpty()) {
			json.append(",\n \"whiteList\" : {\"uris\":[");

			for (Iterator<String> it = project.getWhiteListUrls().iterator();it.hasNext();) {
				String url = it.next();
				json.append("\""+url+"\"");
				if (it.hasNext()){
					json.append(",");
				}
			}
			json.append("]\n");
			json.append("                 }\n");
		}

		json.append("}\n");
		jsonHelper.assertValidJson(json.toString());
		/* @formatter:on */
        getRestHelper().postJSon(getUrlBuilder().buildAdminCreatesProjectUrl(), json.toString());

    }

    
    public void createProductExecutionProfile(String profileId, TestExecutionProfile profile) {
        String url = getUrlBuilder().buildAdminCreatesProductExecutionProfile(profileId);
        String json = JSONConverter.get().toJSON(profile);
        getRestHelper().postJSon(url, json);
    }
    
    public void updateProductExecutionProfile(String profileId, TestExecutionProfile profile) {
        String url = getUrlBuilder().buildAdminUpdatesProductExecutionProfile(profileId);
        String json = JSONConverter.get().toJSON(profile);
        getRestHelper().putJSon(url, json);
    }
    
    public TestExecutionProfile fetchProductExecutionProfile(String profileId) {
        String url = getUrlBuilder().buildAdminFetchesProductExecutionProfile(profileId);
        String json = getRestHelper().getJSon(url);
        return JSONConverter.get().fromJSON(TestExecutionProfile.class, json);
    }
    
    
    public TestExecutionProfileList fetchProductExecutionProfiles() {
        String url = getUrlBuilder().buildAdminFetchesListOfProductExecutionProfiles();
        String json = getRestHelper().getJSon(url);
        return JSONConverter.get().fromJSON(TestExecutionProfileList.class, json);
    }
    
    public void  deleteProductExecutionProfile(String profileId) {
        String url = getUrlBuilder().buildAdminDeletesProductExecutionProfile(profileId);
        getRestHelper().delete(url);
    }
    
    public UUID createProductExecutorConfig(TestExecutorConfig config) {
        String url = getUrlBuilder().buildAdminCreatesProductExecutorConfig();
        String json = JSONConverter.get().toJSON(config);
        String result = getRestHelper().postJSon(url, json);
        return UUID.fromString(result);
    }

    public String fetchProductExecutorConfigAsJSON(UUID uuid) {
        String url = getUrlBuilder().buildAdminFetchesProductExecutorConfig(uuid);
        return getRestHelper().getJSon(url);
    }

    public TestExecutorConfig fetchProductExecutorConfig(UUID uuid) {
        String json = fetchProductExecutorConfigAsJSON(uuid);
        TestExecutorConfig result = JSONConverter.get().fromJSON(TestExecutorConfig.class, json);
        return result;
    }
    
    public void updateProdcutExecutorConfig(UUID uuid, TestExecutorConfig config) {
        String url = getUrlBuilder().buildAdminUpdatesProductExecutorConfig(uuid);
        String json = JSONConverter.get().toJSON(config);
        getRestHelper().putJSon(url, json);
    }
    
    public void deleteProductExecutorConfig(UUID uuid) {
        String url = getUrlBuilder().buildAdminDeletesProductExecutorConfig(uuid);
        getRestHelper().delete(url);
    }

    public void addProjectToProfile(String profileId, TestProject ... projects) {
        /* currently we do this by calling update REST - maybe later we got a dedicated REST method */
        TestExecutionProfile loadedProfile = fetchProductExecutionProfile(profileId);
        for (TestProject project: projects) {
            loadedProfile.projectIds.add(project.getProjectId());
        }
        updateProductExecutionProfile(profileId, loadedProfile);
    }
    
    
    
    
    
    
    public String getServerURL() {
        return getUrlBuilder().buildServerURL();
    }

    public String getStringFromURL(String link) {
        return getRestHelper().getStringFromURL(link);
    }

    /**
     * Assigns user to a project
     *
     * @param targetUser
     * @param project
     * @return this
     */
    public AsUser assignUserToProject(TestUser targetUser, TestProject project) {
        LOG.debug("assigning user:{} to project:{}", user.getUserId(), project.getProjectId());
        getRestHelper().postJSon(getUrlBuilder().buildAdminAssignsUserToProjectUrl(targetUser.getUserId(), project.getProjectId()), "");
        return this;
    }

    /**
     * Unassigns user from project
     *
     * @param targetUser
     * @param project
     * @return this
     */
    public AsUser unassignUserFromProject(TestUser targetUser, TestProject project) {
        LOG.debug("unassigning user:{} from project:{}", user.getUserId(), project.getProjectId());
        getRestHelper().delete(getUrlBuilder().buildAdminUnassignsUserFromProjectUrl(targetUser.getUserId(), project.getProjectId()));
        return this;
    }

    private String createCodeScanJob(TestProject project, IntegrationTestMockMode runMode) {
        String folder = null;
        if (runMode != null) {
            folder = runMode.getTarget();
        }
        if (folder == null) {
            folder = "notexisting";
        }
        String testfile = "sechub-integrationtest-sourcescanconfig1.json";
        String json = IntegrationTestFileSupport.getTestfileSupport().loadTestFile(testfile);
        String projectId = project.getProjectId();

        json = json.replaceAll("__projectId__", projectId);

        json = json.replaceAll("__folder__", folder);
        String url = getUrlBuilder().buildAddJobUrl(projectId);
        return getRestHelper().postJSon(url, json);
    }

    private String createWebScanJob(TestProject project, IntegrationTestMockMode runMode) {
        String json = IntegrationTestFileSupport.getTestfileSupport().loadTestFile("sechub-integrationtest-webscanconfig1.json");
        String projectId = project.getProjectId();

        json = json.replaceAll("__projectId__", projectId);
        List<String> whites = project.getWhiteListUrls();
        String acceptedURI1 = createTargetURIForSechubConfiguration(runMode, whites);

        json = json.replaceAll("__acceptedUri1__", acceptedURI1);
        String url = getUrlBuilder().buildAddJobUrl(projectId);
        return getRestHelper().postJSon(url, json);
    }

    /**
     * Create taget uri - will either use
     *
     * @param runMode
     * @param whites
     * @return
     */
    private String createTargetURIForSechubConfiguration(IntegrationTestMockMode runMode, List<String> whites) {
        String acceptedURI1 = null;
        if (runMode != null) {
            acceptedURI1 = runMode.getTarget();
        }
        if (acceptedURI1 != null) {
            return acceptedURI1;
        }
        if (whites == null || whites.isEmpty()) {
            return "https://undefined.com";
        }
        /* okay, no runmode used having whitelist entry */
        List<String> copy = new ArrayList<>(whites);
        for (IntegrationTestMockMode mode : IntegrationTestMockMode.values()) {
            String target = mode.getTarget();
            if (target != null) {
                /* we drop all existing run mode parts here - to avoid side effects */
                copy.remove(target);
            }
        }
        return copy.iterator().next();
    }

    public void approveJob(TestProject project, UUID jobUUID) {
        getRestHelper().put(getUrlBuilder().buildApproveJobUrl(project.getProjectId(), jobUUID.toString()));
    }

    public AsUser updateWhiteListForProject(TestProject project, List<String> uris) {
        String json = IntegrationTestFileSupport.getTestfileSupport().loadTestFile("sechub-integrationtest-updatewhitelist1.json");
        StringBuilder sb = new StringBuilder();
        for (Iterator<String> it = uris.iterator(); it.hasNext();) {
            sb.append("\\\"");
            sb.append(it.next());
            sb.append("\\\"");
            if (it.hasNext()) {
                sb.append(" , ");
            }
        }
        json = json.replaceAll("__acceptedUris__", sb.toString());
        getRestHelper().postJSon(getUrlBuilder().buildUpdateProjectWhiteListUrl(project.getProjectId()), json);
        return this;

    }

    public String getJobStatus(String projectId, UUID jobUUID) {
        return getRestHelper().getJSon(getUrlBuilder().buildGetJobStatusUrl(projectId, jobUUID.toString()));
    }

    public String getJobReport(String projectId, UUID jobUUID) {
        long waitTimeInMillis = 1000;
        int count = 0;
        boolean jobEnded = false;
        String jobstatus = null;
        while (count < 10) {
            jobstatus = getJobStatus(projectId, jobUUID);
            if (jobstatus.indexOf("ENDED") != -1) {
                jobEnded = true;
                break;
            }
            TestUtil.waitMilliseconds(waitTimeInMillis);
            ++count;
        }
        if (!jobEnded) {
            throw new IllegalStateException("Even after " + count + " retries, every waiting " + waitTimeInMillis
                    + " ms, no job report state ENDED was accessible!\nLAST fetched jobstatus for " + jobUUID + " in project " + projectId + " was:\n"
                    + jobstatus);
        }
        /* okay report is available - so do download */
        return getRestHelper().getJSon(getUrlBuilder().buildGetJobReportUrl(projectId, jobUUID));
    }

    /**
     * When not changed by project specific mock data setup this will result in a
     * RED traffic light result
     * 
     * @param project
     * @return execution result
     */
    public AssertExecutionResult createWebScanAndFetchScanData(TestProject project) {
        ExecutionResult result = withSecHubClient().startSynchronScanFor(project, IntegrationTestJSONLocation.JSON_WEBSCAN_RED);
        return AssertExecutionResult.assertResult(result);
    }

    public String restartCodeScanAndFetchJobStatus(TestProject project, UUID sechubJobUUID) {
        restartJob(sechubJobUUID);
        waitForJobDone(project, sechubJobUUID);
        return getJobStatus(project.getProjectId(), sechubJobUUID);
    }

    public String restartCodeScanHardAndFetchJobStatus(TestProject project, UUID sechubJobUUID) {
        restartJobHard(sechubJobUUID);
        waitForJobDone(project, sechubJobUUID);
        return getJobStatus(project.getProjectId(), sechubJobUUID);
    }

    /**
     * Creates a webscan job for project (but job is not approved, so will not be
     * started)
     *
     * @param project
     * @return uuid for created job
     */
    public UUID createWebScan(TestProject project) {
        return createWebScan(project, null);
    }

    /**
     * Creates a webscan job for project (but job is not approved, so will not be
     * started)
     * 
     * @param project
     * @param useLongRunningButGreen
     * @return
     */
    public UUID createWebScan(TestProject project, IntegrationTestMockMode runMode) {
        assertProject(project).doesExist();
        if (runMode == null) {
            runMode = IntegrationTestMockMode.WEBSCAN__NETSPARKER_RESULT_GREEN__FAST;
        }
        String response = createWebScanJob(project, runMode);
        try {
            JsonNode jsonNode = JSONTestSupport.DEFAULT.fromJson(response);
            JsonNode jobId = jsonNode.get("jobId");
            if (jobId == null) {
                fail("No jobID entry found in json:\n" + response);
                return null;
            }
            return UUID.fromString(jobId.textValue());
        } catch (IllegalArgumentException e) {
            fail("Job did not return with a valid UUID!:" + response);
            throw new IllegalStateException("fail not working");
        } catch (IOException e) {
            throw new IllegalStateException("io failure, should not occure", e);
        }

    }

    /**
     * Creates a code scan job and returns corresponding job UUID. But job is NOT
     * approved and so not started!
     * 
     * @param project
     * @param runMode
     * @return job UUID
     */
    public UUID createCodeScan(TestProject project, IntegrationTestMockMode runMode) {
        assertProject(project).doesExist();
        if (runMode == null) {
            runMode = IntegrationTestMockMode.CODE_SCAN__CHECKMARX__YELLOW__FAST;
        }
        String response = createCodeScanJob(project, runMode);
        try {
            JsonNode jsonNode = JSONTestSupport.DEFAULT.fromJson(response);
            JsonNode jobId = jsonNode.get("jobId");
            if (jobId == null) {
                fail("No jobID entry found in json:\n" + response);
                return null;
            }
            return UUID.fromString(jobId.textValue());
        } catch (IllegalArgumentException e) {
            fail("Job did not return with a valid UUID!:" + response);
            throw new IllegalStateException("fail not working");
        } catch (IOException e) {
            throw new IllegalStateException("io failure, should not occure", e);
        }

    }

    public File downloadAsTempFileFromURL(String url, UUID jobUUID) {
        String fileName = "sechub-file-redownload-" + jobUUID.toString();
        String fileEnding = ".zip";
        return downloadAsTempFileFromURL(url, jobUUID, fileName, fileEnding);
    }

    public File downloadAsTempFileFromURL(String url, UUID jobUUID, String fileName, String fileEnding) {

        // Optional Accept header
        RequestCallback requestCallback = request -> request.getHeaders().setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));

        ResponseExtractor<File> responseExtractor = response -> {
            Path path = Files.createTempFile(fileName, fileEnding);
            Files.copy(response.getBody(), path, StandardCopyOption.REPLACE_EXISTING);
            if (TestUtil.isDeletingTempFiles()) {
                path.toFile().deleteOnExit();
            }
            return path.toFile();
        };
        RestTemplate template = getRestHelper().getTemplate();
        File x = template.execute(url, HttpMethod.GET, requestCallback, responseExtractor);
        return x;
    }

    public String getServerVersion() {
        return getRestHelper().getJSon(getUrlBuilder().buildGetServerVersionUrl());
    }

    public boolean getIsAlive() {
        getRestHelper().head(getUrlBuilder().buildCheckIsAliveUrl());
        return true;
    }

    public AssertFullScanData downloadFullScanDataFor(UUID sechubJobUUID) {
        String url = getUrlBuilder().buildAdminDownloadsZipFileContainingFullScanDataFor(sechubJobUUID);
        File file = downloadAsTempFileFromURL(url, sechubJobUUID, "download-fullscan", ".zip");
        return new AssertFullScanData(file);
    }

    public AsUser grantSuperAdminRightsTo(TestUser targetUser) {
        String url = getUrlBuilder().buildAdminGrantsSuperAdminRightsTo(targetUser.getUserId());
        getRestHelper().post(url);
        return this;
    }

    public AsUser revokeSuperAdminRightsFrom(TestUser targetUser) {
        String url = getUrlBuilder().buildAdminRevokesSuperAdminRightsFrom(targetUser.getUserId());
        getRestHelper().post(url);
        return this;
    }

    public String getScanLogsForProject(TestProject project1) {
        String url = getUrlBuilder().buildAdminFetchesScanLogsForProject(project1.getProjectId());
        return getRestHelper().getJSon(url);
    }

    /**
     * Disbles job processing by scheduler.<br>
     * <br>
     * <b> WARNING:</b> You must ensure that your test will do a
     * <code>as(SUPER_ADMIN).enableSchedulerJobProcessing();</code> at the end of
     * your test (no matter if test fails somewhere in your test case), otherwise
     * you got a extreme side effect to your other integration tests...
     *
     * @return
     */
    public AsUser disableSchedulerJobProcessing() {
        String url = getUrlBuilder().buildAdminDisablesSchedulerJobProcessing();
        getRestHelper().post(url);
        return this;
    }

    public AsUser enableSchedulerJobProcessing() {
        String url = getUrlBuilder().buildAdminEnablesSchedulerJobProcessing();
        getRestHelper().post(url);
        return this;
    }

    public AsUser deleteProject(TestProject project) {
        String url = getUrlBuilder().buildAdminDeletesProject(project.getProjectId());
        getRestHelper().delete(url);
        return this;

    }

    public AsUser cancelJob(UUID jobUUID) {
        String url = getUrlBuilder().buildAdminCancelsJob(jobUUID);
        getRestHelper().post(url);
        return this;
    }

    public AsUser setProjectMockConfiguration(TestProject project, String json) {
        String url = getUrlBuilder().buildSetProjectMockConfiguration(project.getProjectId());
        getRestHelper().putJSon(url, json);
        return this;
    }

    public String getProjectMockConfiguration(TestProject project1) {
        String url = getUrlBuilder().buildGetProjectMockConfiguration(project1.getProjectId());
        return getRestHelper().getJSon(url);

    }

    public AsUser updateMapping(String mappingId, MappingData mappingData) {
        String url = getUrlBuilder().buildUpdateMapping(mappingId);
        getRestHelper().putJSon(url, mappingData.toJSON());
        return this;
    }

    public MappingData getMappingData(String mappingId) {
        String url = getUrlBuilder().buildGetMapping(mappingId);
        return MappingData.fromString(getRestHelper().getJSon(url));
    }

    public ProjectFalsePositivesDefinition getFalsePositiveConfigurationOfProject(TestProject project) {

        String url = getUrlBuilder().buildGetFalsePositiveConfigurationOfProject(project.getProjectId());
        String json = getRestHelper().getJSon(url);

        return create(project, json);

    }

    public AsUser restartJob(UUID jobUUID) {
        String url = getUrlBuilder().buildAdminRestartsJob(jobUUID);
        getRestHelper().post(url);
        return this;

    }

    public AsUser restartJobHard(UUID jobUUID) {
        String url = getUrlBuilder().buildAdminRestartsJobHard(jobUUID);
        getRestHelper().post(url);
        return this;

    }

    public ProjectFalsePositivesDefinition startFalsePositiveDefinition(TestProject project) {
        return new ProjectFalsePositivesDefinition(project);
    }

    public UUID triggerAsyncCodeScanGreenSuperFastWithPseudoZipUpload(TestProject project) {
        return triggerAsyncCodeScanApproveWithoutSourceUploadAndGetJobUUID(project, IntegrationTestMockMode.CODE_SCAN__CHECKMARX__GREEN__SUPERFAST,
                "zipfile_contains_only_test1.txt.zip");
    }

    public UUID triggerAsyncCodeScanWithPseudoZipUpload(TestProject project, IntegrationTestMockMode mode) {
        return triggerAsyncCodeScanApproveWithoutSourceUploadAndGetJobUUID(project, mode, "zipfile_contains_only_test1.txt.zip");
    }

    public UUID triggerAsyncCodeScanApproveWithoutSourceUploadAndGetJobUUID(TestProject project, IntegrationTestMockMode mode, String pathInsideResources) {
        UUID uuid = triggerAsyncScanAndGetJobUUID(project, mode);
        upload(project, uuid, pathInsideResources);

        approveJob(project, uuid);
        return uuid;
    }

    public UUID triggerAsyncScanAndGetJobUUID(TestProject project, IntegrationTestMockMode runMode) {
        UUID uuid = createCodeScan(project, runMode);
        assertNotNull(uuid);
        return uuid;
    }

    public ProjectFalsePositivesDefinition create(TestProject project, String json) {
        ProjectFalsePositivesDefinition def = new ProjectFalsePositivesDefinition(project);

        try {
            JsonNode jsonNode = jsonTestSupport.fromJson(json);
            ArrayNode falsePositives = (ArrayNode) jsonNode.get("falsePositives");
            if (falsePositives == null) {
                fail("No false positives found in json:" + json);
            }
            for (JsonNode falsePositive : falsePositives) {
                JsonNode jobData = falsePositive.get("jobData");

                String jobUUID = jobData.get("jobUUID").asText();
                int findingId = jobData.get("findingId").asInt();

                JsonNode commentNode = jobData.get("comment");
                String comment = null;
                if (commentNode != null) {
                    comment = commentNode.asText();
                }
                def.add(findingId, UUID.fromString(jobUUID), comment);

            }
        } catch (IOException e) {
            throw new IllegalStateException("JSON not valid", e);
        }
        return def;
    }

    public class ProjectFalsePositivesDefinition {

        private TestProject project;

        private class JobData {
            private int findingId;
            private UUID jobUUID;
            private String comment;
        }

        private List<JobData> jobData = new ArrayList<>();
        private WithSecHubClient withSechubClient;
        private IntegrationTestJSONLocation location;

        public ProjectFalsePositivesDefinition(TestProject project) {
            this(project, null, null);
        }

        public ProjectFalsePositivesDefinition(TestProject project, WithSecHubClient withSechubClient, IntegrationTestJSONLocation location) {
            this.project = project;
            this.withSechubClient = withSechubClient;
            this.location = location;
        }

        public boolean isContaining(int findingId, UUID jobUUID) {
            JobData found = findJobData(findingId, jobUUID);
            if (found == null) {
                return false;
            }
            return true;
        }

        public JobData findJobData(int findingId, UUID jobUUID) {
            for (JobData d : jobData) {
                if (d.findingId != findingId) {
                    continue;
                }
                if (!(d.jobUUID.equals(jobUUID))) {
                    continue;
                }
                return d;
            }
            return null;
        }

        public void markAsFalsePositive() {
            if (withSechubClient == null) {
                markAsFalsePositiveByREST();
            } else {
                markFalsePositiveBySecHubClient();
            }
        }

        private void markFalsePositiveBySecHubClient() {
            String json = buildJSON();

            IntegrationTestFileSupport testfileSupport = IntegrationTestFileSupport.getTestfileSupport();
            File file = testfileSupport.createTempFile("mark_as_false_positive", ".json");
            testfileSupport.writeTextFile(file, json);

            withSechubClient.markAsFalsePositive(project, location, file.getAbsolutePath());

        }

        private void markAsFalsePositiveByREST() {
            String json = buildJSON();

            String url = getUrlBuilder().buildUserAddsFalsePositiveJobDataListForProject(project.getProjectId());
            getRestHelper().putJSon(url, json);
        }

        public void unmarkFalsePositive() {
            if (withSechubClient == null) {
                unmarkFalsePositiveByREST();
            } else {
                unmarkFalsePositiveBySecHubClient();
            }
        }

        private void unmarkFalsePositiveBySecHubClient() {
            String json = buildJSON();
            IntegrationTestFileSupport testfileSupport = IntegrationTestFileSupport.getTestfileSupport();
            File file = testfileSupport.createTempFile("unmark_false_positive", ".json");
            testfileSupport.writeTextFile(file, json);

            withSechubClient.unmarkAsFalsePositive(project, location, file.getAbsolutePath());
        }

        private void unmarkFalsePositiveByREST() {
            Iterator<JobData> it = jobData.iterator();
            while (it.hasNext()) {
                JobData data = it.next();
                String url = getUrlBuilder().buildUserRemovesFalsePositiveEntryFromProject(project.getProjectId(), "" + data.jobUUID, "" + data.findingId);
                getRestHelper().delete(url);
            }

        }

        private String buildJSON() {
            String content = "{\"apiVersion\":\"1.0\",\"type\":\"falsePositiveJobDataList\",\"jobData\":[";
            Iterator<JobData> it = jobData.iterator();
            while (it.hasNext()) {
                JobData data = it.next();
                if (data.comment == null) {
                    content += "{\"jobUUID\":\"" + data.jobUUID.toString() + "\",\"findingId\":" + data.findingId + "}";
                } else {
                    content += "{\"jobUUID\":\"" + data.jobUUID.toString() + "\",\"findingId\":" + data.findingId + ",\"comment\":\"" + data.comment + "\"}";
                }
                if (it.hasNext()) {
                    content += ",";
                }
            }
            content += "]}";
            return content;
        }

        public ProjectFalsePositivesDefinition add(int findingId, UUID jobUUID) {
            return add(findingId, jobUUID, null);
        }

        public ProjectFalsePositivesDefinition add(int findingId, UUID jobUUID, String comment) {

            JobData data = new JobData();
            data.findingId = findingId;
            data.jobUUID = jobUUID;
            data.comment = comment;
            jobData.add(data);
            return this;
        }
    }
    
    

}
