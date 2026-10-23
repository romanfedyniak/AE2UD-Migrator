package com.ae2udmigrator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

import com.ae2udmigrator.ae2fc.Ae2fcMigration;
import com.ae2udmigrator.nae2.Nae2Migration;

@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION, useMetadata = true,
        dependencies = AE2UDMigrator.DEPENDENCIES, acceptableRemoteVersions = "*")
public class AE2UDMigrator {

    /**
     * No version on AE2UD: the fork shares its mod id with AE2 and AE2UEL, and the version range that tells
     * them apart waits for AE2UD's first release. The others are where some of the old content goes.
     */
    static final String DEPENDENCIES = "required-after:appliedenergistics2;after:beamformers;after:mekeng;"
            + "after:ae2stuff;after:threng";

    public static final Logger LOG = LogManager.getLogger(Tags.MOD_NAME);

    /**
     * Not earlier: whether a block or item content goes to exists is known only once every mod has registered.
     */
    @Mod.EventHandler
    public void init(final FMLInitializationEvent event) {
        Nae2Migration.register();
        Ae2fcMigration.register();
    }
}
