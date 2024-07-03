/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package tuskex.desktop.main.overlays.windows.downloadupdate;

/**
 * A Task to verify the downloaded tuskex installer against the available keys/signatures.
 */

import com.google.common.collect.Lists;
import javafx.concurrent.Task;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

@Slf4j
@Getter
public class VerifyTask extends Task<List<TuskexInstaller.VerifyDescriptor>> {
    private final List<TuskexInstaller.FileDescriptor> fileDescriptors;

    /**
     * Prepares a task to download a file from {@code fileDescriptors} to {@code saveDir}.
     *
     * @param fileDescriptors HTTP URL of the file to be downloaded
     */
    public VerifyTask(final List<TuskexInstaller.FileDescriptor> fileDescriptors) {
        super();
        this.fileDescriptors = fileDescriptors;
        log.info("Starting VerifyTask with files:{}", fileDescriptors);
    }

    /**
     * Starts the task and therefore the actual download.
     *
     * @return A reference to the created file or {@code null} if no file could be found at the provided URL
     * @throws IOException Forwarded exceotions from HttpURLConnection and file handling methods
     */
    @Override
    protected List<TuskexInstaller.VerifyDescriptor> call() {
        log.debug("VerifyTask started...");
        Optional<TuskexInstaller.FileDescriptor> installer = fileDescriptors.stream()
                .filter(fileDescriptor -> TuskexInstaller.DownloadType.INSTALLER.equals(fileDescriptor.getType()))
                .findFirst();
        if (!installer.isPresent()) {
            log.error("No installer file found.");
            return Lists.newArrayList();
        }

        Optional<TuskexInstaller.FileDescriptor> signingKeyOptional = fileDescriptors.stream()
                .filter(fileDescriptor -> TuskexInstaller.DownloadType.SIGNING_KEY.equals(fileDescriptor.getType()))
                .findAny();

        List<TuskexInstaller.VerifyDescriptor> verifyDescriptors = Lists.newArrayList();
        if (signingKeyOptional.isPresent()) {
            final TuskexInstaller.FileDescriptor signingKeyFD = signingKeyOptional.get();
            StringBuilder sb = new StringBuilder();
            try {
                Scanner scanner = new Scanner(new FileReader(signingKeyFD.getSaveFile()));
                while (scanner.hasNext()) {
                    sb.append(scanner.next());
                }
                scanner.close();
            } catch (Exception e) {
                log.error(e.toString());
                e.printStackTrace();
                TuskexInstaller.VerifyDescriptor.VerifyDescriptorBuilder verifyDescriptorBuilder = TuskexInstaller.VerifyDescriptor.builder();
                verifyDescriptorBuilder.verifyStatusEnum(TuskexInstaller.VerifyStatusEnum.FAIL);
                verifyDescriptors.add(verifyDescriptorBuilder.build());
                return verifyDescriptors;
            }
            String signingKey = sb.toString();

            List<TuskexInstaller.FileDescriptor> sigs = fileDescriptors.stream()
                    .filter(fileDescriptor -> TuskexInstaller.DownloadType.SIG.equals(fileDescriptor.getType()))
                    .collect(Collectors.toList());

            // iterate all signatures available to us
            for (TuskexInstaller.FileDescriptor sig : sigs) {
                TuskexInstaller.VerifyDescriptor.VerifyDescriptorBuilder verifyDescriptorBuilder = TuskexInstaller.VerifyDescriptor.builder().sigFile(sig.getSaveFile());
                // Sigs are linked to keys, extract all keys which have the same id
                List<TuskexInstaller.FileDescriptor> keys = fileDescriptors.stream()
                        .filter(keyDescriptor -> TuskexInstaller.DownloadType.KEY.equals(keyDescriptor.getType()))
                        .filter(keyDescriptor -> sig.getId().equals(keyDescriptor.getId()))
                        .collect(Collectors.toList());
                // iterate all keys which have the same id
                for (TuskexInstaller.FileDescriptor key : keys) {
                    if (signingKey.equals(key.getId())) {
                        verifyDescriptorBuilder.keyFile(key.getSaveFile());
                        try {
                            verifyDescriptorBuilder.verifyStatusEnum(TuskexInstaller.verifySignature(key.getSaveFile(),
                                    sig.getSaveFile(),
                                    installer.get().getSaveFile()));
                            updateMessage(key.getFileName());
                        } catch (Exception e) {
                            verifyDescriptorBuilder.verifyStatusEnum(TuskexInstaller.VerifyStatusEnum.FAIL);
                            log.error(e.toString());
                            e.printStackTrace();
                        }
                        verifyDescriptors.add(verifyDescriptorBuilder.build());
                    } else {
                        log.trace("key not matching the defined in signingKey. We try the next.");
                    }
                }
            }
        } else {
            log.error("signingKey is not found");
            TuskexInstaller.VerifyDescriptor.VerifyDescriptorBuilder verifyDescriptorBuilder = TuskexInstaller.VerifyDescriptor.builder();
            verifyDescriptorBuilder.verifyStatusEnum(TuskexInstaller.VerifyStatusEnum.FAIL);
            verifyDescriptors.add(verifyDescriptorBuilder.build());
        }

        return verifyDescriptors;
    }
}

