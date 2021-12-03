package io.harness.serializer.kryo;

import io.harness.licensing.Edition;
import io.harness.licensing.LicenseStatus;
import io.harness.licensing.LicenseType;
import io.harness.licensing.beans.modules.CDModuleLicenseDTO;
import io.harness.licensing.beans.modules.CEModuleLicenseDTO;
import io.harness.licensing.beans.modules.CFModuleLicenseDTO;
import io.harness.licensing.beans.modules.CIModuleLicenseDTO;
import io.harness.licensing.beans.modules.CVModuleLicenseDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.beans.modules.types.CDLicenseType;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

public class LicenseBeanKryoClassesRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(CDModuleLicenseDTO.class, 930001);
    kryo.register(CEModuleLicenseDTO.class, 930002);
    kryo.register(CFModuleLicenseDTO.class, 930003);
    kryo.register(CIModuleLicenseDTO.class, 930004);
    kryo.register(CVModuleLicenseDTO.class, 930005);
    kryo.register(ModuleLicenseDTO.class, 930006);
    kryo.register(Edition.class, 930007);
    kryo.register(LicenseType.class, 930008);
    kryo.register(LicenseStatus.class, 930009);
    kryo.register(CDLicenseType.class, 930010);
  }
}
