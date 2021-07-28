package de.samply.share.broker.utils.connector;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class IcingaReportItem {

  @SerializedName("exit_status")
  private String exitStatus;
  @SerializedName("plugin_output")
  private String pluginOutput;
  private List<IcingaPerformanceData> performanceData;

  public IcingaReportItem() {
    performanceData = new ArrayList<>();
  }

  public void setExitStatus(String exitStatus) {
    this.exitStatus = exitStatus;
  }

  public void setPluginOutput(String pluginOutput) {
    this.pluginOutput = pluginOutput;
  }

  public List<IcingaPerformanceData> getPerformanceData() {
    return performanceData;
  }

}
