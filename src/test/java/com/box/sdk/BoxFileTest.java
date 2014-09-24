package com.box.sdk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class BoxFileTest {
    @Test
    @Category(IntegrationTest.class)
    public void downloadFileSucceeds() throws UnsupportedEncodingException {
        BoxAPIConnection api = new BoxAPIConnection(TestConfig.getAccessToken());
        BoxFolder rootFolder = BoxFolder.getRootFolder(api);

        final String fileContent = "Test file";
        final long fileLength = fileContent.length();
        InputStream stream = new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8));
        BoxFile uploadedFile = rootFolder.uploadFile(stream, "Test File.txt", null, null);
        assertThat(rootFolder, hasItem(uploadedFile));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        final boolean[] onProgressChangedCalled = new boolean[]{false};
        uploadedFile.download(output, new ProgressListener() {
            public void onProgressChanged(long numBytes, long totalBytes) {
                onProgressChangedCalled[0] = true;

                assertThat(numBytes, is(not(0L)));
                assertThat(totalBytes, is(equalTo(fileLength)));
            }
        });

        assertThat(onProgressChangedCalled[0], is(true));
        String downloadedFileContent = output.toString(StandardCharsets.UTF_8.name());
        assertThat(downloadedFileContent, equalTo(fileContent));

        uploadedFile.delete();
        assertThat(rootFolder, not(hasItem(uploadedFile)));
    }

    @Test
    @Category(IntegrationTest.class)
    public void downloadVersionSucceeds() throws UnsupportedEncodingException {
        BoxAPIConnection api = new BoxAPIConnection(TestConfig.getAccessToken());
        BoxFolder rootFolder = BoxFolder.getRootFolder(api);

        final String fileName = "[downloadVersionSucceeds] Multi-version File.txt";
        final String version1Content = "Version 1";
        final long version1Length =  version1Content.length();
        final String version2Content = "Version 2";

        InputStream stream = new ByteArrayInputStream(version1Content.getBytes(StandardCharsets.UTF_8));
        BoxFile uploadedFile = rootFolder.uploadFile(stream, fileName);
        assertThat(rootFolder, hasItem(uploadedFile));

        stream = new ByteArrayInputStream(version2Content.getBytes(StandardCharsets.UTF_8));
        uploadedFile.uploadVersion(stream);

        Collection<BoxFileVersion> versions = uploadedFile.getVersions();
        BoxFileVersion previousVersion = versions.iterator().next();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        final boolean[] onProgressChangedCalled = new boolean[]{false};
        previousVersion.download(output, new ProgressListener() {
            public void onProgressChanged(long numBytes, long totalBytes) {
                onProgressChangedCalled[0] = true;

                assertThat(numBytes, is(not(0L)));
                assertThat(totalBytes, is(equalTo(version1Length)));
            }
        });

        assertThat(onProgressChangedCalled[0], is(true));
        String downloadedFileContent = output.toString(StandardCharsets.UTF_8.name());
        assertThat(downloadedFileContent, equalTo(version1Content));

        uploadedFile.delete();
        assertThat(rootFolder, not(hasItem(uploadedFile)));
    }

    @Test
    @Category(IntegrationTest.class)
    public void getInfoWithOnlyTheNameField() {
        final String expectedName = "[getInfoWithOnlyTheNameField] Test File.txt";

        BoxAPIConnection api = new BoxAPIConnection(TestConfig.getAccessToken());
        BoxFolder rootFolder = BoxFolder.getRootFolder(api);

        final String fileContent = "Test file";
        InputStream stream = new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8));
        BoxFile uploadedFile = rootFolder.uploadFile(stream, expectedName, null, null);
        assertThat(rootFolder, hasItem(uploadedFile));

        BoxFile.Info info = uploadedFile.getInfo("name");
        final String actualName = info.getName();
        final String actualDescription = info.getDescription();
        final long actualSize = info.getSize();

        assertThat(expectedName, equalTo(actualName));
        assertThat(actualDescription, is(nullValue()));
        assertThat(actualSize, is(0L));

        uploadedFile.delete();
        assertThat(rootFolder, not(hasItem(uploadedFile)));
    }

    @Test
    @Category(IntegrationTest.class)
    public void updateFileInfoSucceeds() {
        final String originalName = "[updateFileInfoSucceeds] Original Name.txt";
        final String newName = "[updateFileInfoSucceeds] New Name.txt";

        BoxAPIConnection api = new BoxAPIConnection(TestConfig.getAccessToken());
        BoxFolder rootFolder = BoxFolder.getRootFolder(api);

        final String fileContent = "Test file";
        InputStream stream = new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8));
        BoxFile uploadedFile = rootFolder.uploadFile(stream, originalName, null, null);
        assertThat(rootFolder, hasItem(uploadedFile));

        BoxFile.Info info = uploadedFile.new Info();
        info.setName(newName);
        uploadedFile.updateInfo(info);

        info = uploadedFile.getInfo();
        assertThat(info.getName(), equalTo(newName));

        uploadedFile.delete();
        assertThat(rootFolder, not(hasItem(uploadedFile)));
    }

    @Test
    @Category(IntegrationTest.class)
    public void uploadMultipleVersionsSucceeds() throws UnsupportedEncodingException {
        BoxAPIConnection api = new BoxAPIConnection(TestConfig.getAccessToken());
        BoxFolder rootFolder = BoxFolder.getRootFolder(api);

        final String fileName = "[uploadMultipleVersionsSucceeds] Multi-version File.txt";
        final String version1Content = "Version 1";
        final String version1Sha = "db3cbc01da600701b9fe4a497fe328e71fa7022f";
        final String version2Content = "Version 2";

        InputStream stream = new ByteArrayInputStream(version1Content.getBytes(StandardCharsets.UTF_8));
        BoxFile file = rootFolder.uploadFile(stream, fileName);
        assertThat(rootFolder, hasItem(file));

        stream = new ByteArrayInputStream(version2Content.getBytes(StandardCharsets.UTF_8));
        file.uploadVersion(stream);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        file.download(output);
        String downloadedFileContent = output.toString(StandardCharsets.UTF_8.name());
        assertThat(downloadedFileContent, equalTo(version2Content));

        Collection<BoxFileVersion> versions = file.getVersions();
        assertThat(versions, hasSize(1));

        BoxFileVersion previousVersion = versions.iterator().next();
        assertThat(previousVersion.getSha1(), is(equalTo(version1Sha)));

        file.delete();
        assertThat(rootFolder, not(hasItem(file)));
    }

    @Test
    @Category(IntegrationTest.class)
    public void deleteVersionDoesNotThrowException() throws UnsupportedEncodingException {
        BoxAPIConnection api = new BoxAPIConnection(TestConfig.getAccessToken());
        BoxFolder rootFolder = BoxFolder.getRootFolder(api);

        final String fileName = "[deleteVersionSucceeds] Multi-version File.txt";
        final String version1Content = "Version 1";
        final String version2Content = "Version 2";

        InputStream stream = new ByteArrayInputStream(version1Content.getBytes(StandardCharsets.UTF_8));
        BoxFile file = rootFolder.uploadFile(stream, fileName);
        assertThat(rootFolder, hasItem(file));

        stream = new ByteArrayInputStream(version2Content.getBytes(StandardCharsets.UTF_8));
        file.uploadVersion(stream);

        Collection<BoxFileVersion> versions = file.getVersions();
        assertThat(versions, hasSize(1));

        BoxFileVersion previousVersion = versions.iterator().next();
        previousVersion.delete();

        file.delete();
        assertThat(rootFolder, not(hasItem(file)));
    }

    @Test
    @Category(IntegrationTest.class)
    public void promoteVersionsSucceeds() throws UnsupportedEncodingException {
        BoxAPIConnection api = new BoxAPIConnection(TestConfig.getAccessToken());
        BoxFolder rootFolder = BoxFolder.getRootFolder(api);

        final String fileName = "[promoteVersionsSucceeds] Multi-version File.txt";
        final String version1Content = "Version 1";
        final String version2Content = "Version 2";

        InputStream stream = new ByteArrayInputStream(version1Content.getBytes(StandardCharsets.UTF_8));
        BoxFile file = rootFolder.uploadFile(stream, fileName);
        assertThat(rootFolder, hasItem(file));

        stream = new ByteArrayInputStream(version2Content.getBytes(StandardCharsets.UTF_8));
        file.uploadVersion(stream);

        Collection<BoxFileVersion> versions = file.getVersions();
        BoxFileVersion previousVersion = versions.iterator().next();
        previousVersion.promote();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        file.download(output);
        String downloadedFileContent = output.toString(StandardCharsets.UTF_8.name());
        assertThat(downloadedFileContent, equalTo(version1Content));

        file.delete();
        assertThat(rootFolder, not(hasItem(file)));
    }

    @Test
    @Category(IntegrationTest.class)
    public void copyFileSucceeds() throws UnsupportedEncodingException {
        BoxAPIConnection api = new BoxAPIConnection(TestConfig.getAccessToken());
        BoxFolder rootFolder = BoxFolder.getRootFolder(api);

        final String originalName = "[copyFileSucceeds] Original File.txt";
        final String newName = "[copyFileSucceeds] New File.txt";
        final String fileContent = "Test file";

        InputStream stream = new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8));
        BoxFile uploadedFile = rootFolder.uploadFile(stream, originalName);
        assertThat(rootFolder, hasItem(uploadedFile));

        BoxFile.Info copiedFileInfo = uploadedFile.copy(rootFolder, newName);
        BoxFile copiedFile = copiedFileInfo.getResource();
        assertThat(rootFolder, hasItem(copiedFile));

        uploadedFile.delete();
        assertThat(rootFolder, not(hasItem(uploadedFile)));
        copiedFile.delete();
        assertThat(rootFolder, not(hasItem(copiedFile)));
    }
}
