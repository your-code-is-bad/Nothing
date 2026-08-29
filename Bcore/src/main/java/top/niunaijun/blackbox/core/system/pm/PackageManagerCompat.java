package top.niunaijun.blackbox.core.system.pm;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ConfigurationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.Signature;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;

import java.util.HashSet;
import java.util.Set;

import black.android.content.pm.BRApplicationInfoL;
import black.android.content.pm.BRApplicationInfoN;
import black.android.content.pm.BRPackageParserSigningDetails;
import black.android.content.pm.BRSigningInfo;
import black.android.content.res.BRAssetManager;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.core.env.AppSystemEnv;
import top.niunaijun.blackbox.core.env.BEnvironment;
import top.niunaijun.blackbox.entity.pm.InstallOption;
import top.niunaijun.blackbox.utils.ArrayUtils;
import top.niunaijun.blackbox.utils.FileUtils;
import top.niunaijun.blackbox.utils.compat.BuildCompat;


@SuppressLint({"SdCardPath", "NewApi"})
public class PackageManagerCompat {

    public static PackageInfo generatePackageInfo(BPackageSettings ps, int flags, BPackageUserState state, int userId) {
        if (ps == null) {
            return null;
        }
        BPackage p = ps.pkg;
        if (p != null) {
            PackageInfo packageInfo = null;
            try {
                packageInfo = generatePackageInfo(p, flags, 0, 0, state, userId);
            } catch (Throwable ignored) {
            }
            return packageInfo;
        }
        return null;
    }

    public static PackageInfo generatePackageInfo(BPackage p, int flags, long firstInstallTime, long lastUpdateTime, BPackageUserState state, int userId) {
        if (!checkUseInstalledOrHidden(flags, state, p.applicationInfo)) {
            return null;
        }

        PackageInfo pi = new PackageInfo();
        pi.packageName = p.packageName;
        pi.versionCode = p.mVersionCode;
        pi.versionName = p.mVersionName;
        pi.sharedUserId = p.mSharedUserId;
        pi.sharedUserLabel = p.mSharedUserLabel;
        pi.applicationInfo = generateApplicationInfo(p, flags, state, userId);

        pi.firstInstallTime = firstInstallTime;
        pi.lastUpdateTime = lastUpdateTime;
        if (!p.requestedPermissions.isEmpty()) {
            String[] requestedPermissions = new String[p.requestedPermissions.size()];
            p.requestedPermissions.toArray(requestedPermissions);
            pi.requestedPermissions = requestedPermissions;
        }

        if ((flags & PackageManager.GET_GIDS) != 0) {
            pi.gids = new int[]{};
        }
        if ((flags & PackageManager.GET_CONFIGURATIONS) != 0) {
            int N = p.configPreferences != null ? p.configPreferences.size() : 0;
            if (N > 0) {
                pi.configPreferences = new ConfigurationInfo[N];
                p.configPreferences.toArray(pi.configPreferences);
            }
            N = p.reqFeatures != null ? p.reqFeatures.size() : 0;
            if (N > 0) {
                pi.reqFeatures = new FeatureInfo[N];
                p.reqFeatures.toArray(pi.reqFeatures);
            }
        }
        if ((flags & PackageManager.GET_ACTIVITIES) != 0) {
            pi.activities = null;
            final int N = p.activities.size();
            if (N > 0) {
                int num = 0;
                final ActivityInfo[] res = new ActivityInfo[N];
                for (int i = 0; i < N; i++) {
                    final BPackage.Activity a = p.activities.get(i);
                    res[num++] = generateActivityInfo(a, flags, state, userId);
                }
                pi.activities = ArrayUtils.trimToSize(res, num);
            }
        }
        if ((flags & PackageManager.GET_RECEIVERS) != 0) {
            pi.receivers = null;
            final int N = p.receivers.size();
            if (N > 0) {
                int num = 0;
                final ActivityInfo[] res = new ActivityInfo[N];
                for (int i = 0; i < N; i++) {
                    final BPackage.Activity a = p.receivers.get(i);
                    res[num++] = generateActivityInfo(a, flags, state, userId);
                }
                pi.receivers = ArrayUtils.trimToSize(res, num);
            }
        }
        if ((flags & PackageManager.GET_SERVICES) != 0) {
            pi.services = null;
            final int N = p.services.size();
            if (N > 0) {
                int num = 0;
                final ServiceInfo[] res = new ServiceInfo[N];
                for (int i = 0; i < N; i++) {
                    final BPackage.Service s = p.services.get(i);
                    
                    ServiceInfo serviceInfo = generateServiceInfo(s, flags, state, userId);
                    if(serviceInfo == null) continue;
                    if ((flags & PackageManager.GET_META_DATA) != 0) {
                        if(serviceInfo.metaData != null){
                            res[num++] = serviceInfo;
                        }
                    }else{
                        res[num++] = serviceInfo;
                    }
                }
                pi.services = ArrayUtils.trimToSize(res, num);
            }
        }
        if ((flags & PackageManager.GET_PROVIDERS) != 0) {
            pi.providers = null;
            final int N = p.providers.size();
            if (N > 0) {
                int num = 0;
                final ProviderInfo[] res = new ProviderInfo[N];
                for (int i = 0; i < N; i++) {
                    final BPackage.Provider pr = p.providers.get(i);
                    ProviderInfo providerInfo = generateProviderInfo(pr, flags, state, userId);
                    if (providerInfo != null) {
                        res[num++] = providerInfo;
                    }
                }
                pi.providers = ArrayUtils.trimToSize(res, num);
            }
        }
        if ((flags & PackageManager.GET_INSTRUMENTATION) != 0) {
            pi.instrumentation = null;
            int N = p.instrumentation.size();
            if (N > 0) {
                pi.instrumentation = new InstrumentationInfo[N];
                for (int i = 0; i < N; i++) {
                    pi.instrumentation[i] = generateInstrumentationInfo(
                            p.instrumentation.get(i), flags);
                }
            }
        }
        if ((flags & PackageManager.GET_PERMISSIONS) != 0) {
            pi.permissions = null;
            int N = p.permissions.size();
            if (N > 0) {
                pi.permissions = new PermissionInfo[N];
                for (int i = 0; i < N; i++) {
                    pi.permissions[i] = generatePermissionInfo(p.permissions.get(i), flags);
                }
            }
            pi.requestedPermissions = null;
            N = p.requestedPermissions.size();
            if (N > 0) {
                pi.requestedPermissions = new String[N];
                pi.requestedPermissionsFlags = new int[N];
                for (int i = 0; i < N; i++) {
                    final String perm = p.requestedPermissions.get(i);
                    pi.requestedPermissions[i] = perm;
                    




                }
            }
        }
        PackageInfo base = null;
        try {
            base = BlackBoxCore.getContext().getPackageManager().getPackageInfo(p.packageName, flags);
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        
        Signature[] spoofed = microGSignatureSpoor(p.packageName, base);
        boolean spoofedSig = spoofed != null;

        if ((flags & PackageManager.GET_SIGNATURES) != 0) {
            if (spoofedSig) {
                pi.signatures = spoofed;
            } else if (base == null) {
                pi.signatures = p.mSignatures;
            } else {
                pi.signatures = base.signatures;
            }
        }
        if (BuildCompat.isPie()) {
            if ((flags & PackageManager.GET_SIGNING_CERTIFICATES) != 0) {
                if (spoofedSig) {
                    PackageParser.SigningDetails signingDetails = PackageParser.SigningDetails.UNKNOWN;
                    BRPackageParserSigningDetails.get(signingDetails)._set_signatures(spoofed);
                    pi.signingInfo = BRSigningInfo.get()._new(signingDetails);
                } else if (base == null) {
                    PackageParser.SigningDetails signingDetails = PackageParser.SigningDetails.UNKNOWN;
                    BRPackageParserSigningDetails.get(signingDetails)._set_signatures(p.mSigningDetails.signatures);
                    pi.signingInfo = BRSigningInfo.get()._new(signingDetails);
                } else {
                    pi.signingInfo = base.signingInfo;
                }
            }
        }
        return pi;
    }

    
    private static final String[] MICROG_SIGNATURE_PACKAGES = {
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.android.vending",
            "org.microg.gms"
    };

    
    private static final String GOOGLE_SIGNATURE_HEX =
            "308204433082032ba003020102020900c2e08746644a308d300d06092a864886f70d01010405003074310b3009060355040613025553311330110603550408130a43616c69666f726e6961311630140603550407130d4d6f756e7461696e205669657731143012060355040a130b476f6f676c6520496e632e3110300e060355040b1307416e64726f69643110300e06035504031307416e64726f6964301e170d3038303832313233313333345a170d3336303130373233313333345a3074310b3009060355040613025553311330110603550408130a43616c69666f726e6961311630140603550407130d4d6f756e7461696e205669657731143012060355040a130b476f6f676c6520496e632e3110300e060355040b1307416e64726f69643110300e06035504031307416e64726f696430820120300d06092a864886f70d01010105000382010d00308201080282010100ab562e00d83ba208ae0a966f124e29da11f2ab56d08f58e2cca91303e9b754d372f640a71b1dcb130967624e4656a7776a92193db2e5bfb724a91e77188b0e6a47a43b33d9609b77183145ccdf7b2e586674c9e1565b1f4c6a5955bff251a63dabf9c55c27222252e875e4f8154a645f897168c0b1bfc612eabf785769bb34aa7984dc7e2ea2764cae8307d8c17154d7ee5f64a51a44a602c249054157dc02cd5f5c0e55fbef8519fbe327f0b1511692c5a06f19d18385f5c4dbc2d6b93f68cc2979c70e18ab93866b3bd5db8999552a0e3b4c99df58fb918bedc182ba35e003c1b4b10dd244a8ee24fffd333872ab5221985edab0fc0d0b145b6aa192858e79020103a381d93081d6301d0603551d0e04160414c77d8cc2211756259a7fd382df6be398e4d786a53081a60603551d2304819e30819b8014c77d8cc2211756259a7fd382df6be398e4d786a5a178a4763074310b3009060355040613025553311330110603550408130a43616c69666f726e6961311630140603550407130d4d6f756e7461696e205669657731143012060355040a130b476f6f676c6520496e632e3110300e060355040b1307416e64726f69643110300e06035504031307416e64726f6964820900c2e08746644a308d300c0603551d13040530030101ff300d06092a864886f70d010104050003820101006dd252ceef85302c360aaace939bcff2cca904bb5d7a1661f8ae46b2994204d0ff4a68c7ed1a531ec4595a623ce60763b167297a7ae35712c407f208f0cb109429124d7b106219c084ca3eb3f9ad5fb871ef92269a8be28bf16d44c8d9a08e6cb2f005bb3fe2cb96447e868e731076ad45b33f6009ea19c161e62641aa99271dfd5228c5c587875ddb7f452758d661f6cc0cccb7352e424cc4365c523532f7325137593c4ae341f4db41edda0d0b1071a7c440f0fe9ea01cb627ca674369d084bd2fd911ff06cdbf2cfa10dc0f893ae35762919048c7efc64c7144178342f70581c9de573af55b390dd7fdb9418631895d5f759f30112687ff621410c069308a";

    
    private static Signature[] microGSignatureSpoor(String packageName, PackageInfo base) {
        if (!ArrayUtils.contains(MICROG_SIGNATURE_PACKAGES, packageName)) {
            return null;
        }
        
        if (base != null && base.signatures != null && base.signatures.length > 0) {
            return null;
        }
        try {
            Signature fake = new Signature(GOOGLE_SIGNATURE_HEX);
            return new Signature[]{fake};
        } catch (Throwable t) {
            return null;
        }
    }

    public static ActivityInfo generateActivityInfo(BPackage.Activity a, int flags, BPackageUserState state, int userId) {
        if (!checkUseInstalledOrHidden(flags, state, a.info.applicationInfo)) {
            return null;
        }
        
        ActivityInfo ai = new ActivityInfo(a.info);
        ai.metaData = a.metaData;
        ai.processName = BPackageManagerService.fixProcessName(ai.packageName, ai.processName);
        ai.applicationInfo = generateApplicationInfo(a.owner, flags, state, userId);
        return ai;
    }

    public static ServiceInfo generateServiceInfo(BPackage.Service s, int flags, BPackageUserState state, int userId) {
        if (!checkUseInstalledOrHidden(flags, state, s.info.applicationInfo)) {
            return null;
        }
        
        ServiceInfo si = new ServiceInfo(s.info);
        si.metaData = s.metaData;
        si.processName = BPackageManagerService.fixProcessName(si.packageName, si.processName);
        si.applicationInfo = generateApplicationInfo(s.owner, flags, state, userId);
        return si;
    }

    public static ProviderInfo generateProviderInfo(BPackage.Provider p, int flags, BPackageUserState state, int userId) {
        if (!checkUseInstalledOrHidden(flags, state, p.info.applicationInfo)) {
            return null;
        }
        
        ProviderInfo pi = new ProviderInfo(p.info);
        if (pi.authority == null)
            return null;
        pi.metaData = p.metaData;
        pi.processName = BPackageManagerService.fixProcessName(pi.packageName, pi.processName);
        if ((flags & PackageManager.GET_URI_PERMISSION_PATTERNS) == 0) {
            pi.uriPermissionPatterns = null;
        }
        pi.applicationInfo = generateApplicationInfo(p.owner, flags, state, userId);
        return pi;
    }

    public static PermissionInfo generatePermissionInfo(
            BPackage.Permission p, int flags) {
        if (p == null) return null;
        if ((flags & PackageManager.GET_META_DATA) == 0) {
            return p.info;
        }
        PermissionInfo pi = new PermissionInfo(p.info);
        pi.metaData = p.metaData;
        return pi;
    }

    public static InstrumentationInfo generateInstrumentationInfo(
            BPackage.Instrumentation i, int flags) {
        if (i == null) return null;
        if ((flags & PackageManager.GET_META_DATA) == 0) {
            return i.info;
        }
        InstrumentationInfo ii = new InstrumentationInfo(i.info);
        ii.metaData = i.metaData;
        return ii;
    }

    public static ApplicationInfo generateApplicationInfo(BPackage p, int flags, BPackageUserState state, int userId) {
        if (!checkUseInstalledOrHidden(flags, state, p.applicationInfo)) {
            return null;
        }
        ApplicationInfo baseApplication;
        try {
            baseApplication = BlackBoxCore.getPackageManager().getApplicationInfo(BlackBoxCore.getHostPkg(), flags);
        } catch (Exception e) {
            return null;
        }
        String sourceDir = p.baseCodePath;
        if (p.applicationInfo == null) {
            p.applicationInfo = BlackBoxCore.getPackageManager()
                    .getPackageArchiveInfo(sourceDir, 0).applicationInfo;
        }
        ApplicationInfo ai = new ApplicationInfo(p.applicationInfo);
        if ((flags & PackageManager.GET_META_DATA) != 0) {
            ai.metaData = p.mAppMetaData;
        }
        ai.dataDir = BEnvironment.getDataDir(ai.packageName, userId).getAbsolutePath();
        if (!p.installOption.isFlag(InstallOption.FLAG_SYSTEM)) {
            ai.nativeLibraryDir = BEnvironment.getAppLibDir(ai.packageName).getAbsolutePath();
        }
        ai.processName = BPackageManagerService.fixProcessName(p.packageName, ai.packageName);
        ai.publicSourceDir = sourceDir;
        ai.sourceDir = sourceDir;
        ai.uid = p.mExtras.appId;


        if (BuildCompat.isL()) {
            BRApplicationInfoL.get(ai)._set_primaryCpuAbi(Build.CPU_ABI);
            BRApplicationInfoL.get(ai)._set_scanPublicSourceDir(BRApplicationInfoL.get(baseApplication).scanPublicSourceDir());
            BRApplicationInfoL.get(ai)._set_scanSourceDir(BRApplicationInfoL.get(baseApplication).scanSourceDir());
        }
        if (BuildCompat.isN()) {
            ai.deviceProtectedDataDir = BEnvironment.getDeDataDir(p.packageName, userId).getAbsolutePath();

            if (BRApplicationInfoN.get(ai)._check_deviceEncryptedDataDir() != null) {
                BRApplicationInfoN.get(ai)._set_deviceEncryptedDataDir(ai.deviceProtectedDataDir);
            }
            if (BRApplicationInfoN.get(ai)._check_credentialEncryptedDataDir() != null) {
                BRApplicationInfoN.get(ai)._set_credentialEncryptedDataDir(ai.dataDir);
            }
            if (BRApplicationInfoN.get(ai)._check_deviceProtectedDataDir() != null) {
                BRApplicationInfoN.get(ai)._set_deviceProtectedDataDir(ai.deviceProtectedDataDir);
            }
            if (BRApplicationInfoN.get(ai)._check_credentialProtectedDataDir() != null) {
                BRApplicationInfoN.get(ai)._set_credentialProtectedDataDir(ai.dataDir);
            }
        }
        fixJar(ai);
        return ai;
    }

    private static boolean checkUseInstalledOrHidden(int flags, BPackageUserState state,
                                                     ApplicationInfo appInfo) {
        if (AppSystemEnv.isBlackPackage(appInfo.packageName))
            return false;
        
        if (!state.installed || state.hidden) {
            return false;
        }
        return true;
    }

    private static void fixJar(ApplicationInfo info) {
        String APACHE_LEGACY_JAR = "/system/framework/org.apache.http.legacy.boot.jar";
        String APACHE_LEGACY_JAR_Q = "/system/framework/org.apache.http.legacy.jar";
        Set<String> sharedLibraryFileList = new HashSet<>();
        if (BuildCompat.isQ()) {
            if (!FileUtils.isExist(APACHE_LEGACY_JAR_Q)) {
                sharedLibraryFileList.add(APACHE_LEGACY_JAR);
            } else {
                sharedLibraryFileList.add(APACHE_LEGACY_JAR_Q);
            }
        } else {
            sharedLibraryFileList.add(APACHE_LEGACY_JAR);
        }





        info.sharedLibraryFiles = sharedLibraryFileList.toArray(new String[]{});
    }

    public static Resources getResources(Context context, ApplicationInfo appInfo) {
        BPackageSettings ps = BPackageManagerService.get().getBPackageSetting(appInfo.packageName);
        if (ps != null) {
            AssetManager assets = BRAssetManager.get()._new();
            BRAssetManager.get(assets).addAssetPath(ps.pkg.baseCodePath);
            Resources hostRes = context.getResources();
            return new Resources(assets, hostRes.getDisplayMetrics(), hostRes.getConfiguration());
        }
        return null;
    }
}
