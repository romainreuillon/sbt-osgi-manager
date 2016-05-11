/**
 * sbt-osgi-manager - OSGi development bridge based on Bnd and Tycho.
 *
 * Copyright (c) 2016 Alexey Aksenov ezh@ezh.msk.ru
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sbt.osgi.manager.maven

import java.io.File
import java.net.URI
import org.apache.maven.model.{ Dependency ⇒ MavenDependency }
import org.eclipse.equinox.p2.metadata.IInstallableUnit
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository
import org.eclipse.tycho.artifacts.TargetPlatform
import org.eclipse.tycho.core.ee.shared.ExecutionEnvironmentConfigurationStub
import org.eclipse.tycho.osgi.adapters.MavenLoggerAdapter
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult
import org.scalatest.{ FreeSpec, Matchers }
import sbt.{ AttributeEntry, AttributeMap, BasicCommands, Build, BuildStreams, BuildStructure, BuildUnit, BuildUtil, ConsoleOut, Def, DetectedAutoPlugin, DetectedModules, DetectedPlugins, File, GlobalLogging, KeyIndex, Keys, Load, LoadedDefinitions, LoadedPlugins, MainLogging, PartBuildUnit, Plugin, PluginData, Project, ProjectRef, Scope, SessionSettings, Settings, State, StructureIndex, This }
import sbt.osgi.manager.{ Dependency, OSGi, OSGiConf, OSGiKey, Plugin, Test }
import sbt.osgi.manager.maven.action.ResolveP2
import sbt.toGroupID
import scala.collection.JavaConversions.{ asScalaBuffer, asScalaSet, collectionAsScalaIterable }
import scala.collection.immutable
import scala.language.implicitConversions

class MavenTest extends FreeSpec with Matchers {
  val folder = Test.createTempFolder() getOrElse { throw new IllegalStateException("Unable to create temporary directory") }
  val mavenFolder = new File(folder, "maven")
  val settings = Project.inConfig(OSGiConf)(Seq(
    OSGiKey.osgiMavenDirectory := mavenFolder))

  "test" in {
    Test.withImplementation(ResolveP2, new TestResolveP2) {
      info("Maven environment located at folder: " + mavenFolder)
      Test.removeAll(mavenFolder)
      implicit val arg = Plugin.TaskArgument(FakeState.state, ProjectRef(FakeState.testProject.base.toURI(), FakeState.testProject.id))
      val mavenHome = Maven.prepareHome()
      info("Maven home is " + mavenHome)
      val bridge = Maven()
      bridge should not be (null)

      val dependencies = Seq(Dependency.convertDependency(OSGi.ECLIPSE_PLUGIN % "org.eclipse.ui" % OSGi.ANY_VERSION.toString()))
      val rawRepositories = Seq(("Eclipse P2 update site", new URI("http://eclipse.ialto.com/eclipse/updates/4.2/R-4.2.1-201209141800/")))
      val environment = sbt.osgi.manager.OSGiEnvironmentJRE1_6
      val (targetPlatform, repositories) = ResolveP2.inner.asInstanceOf[TestResolveP2].
        createTargetPlatformAndRepositories(rawRepositories,
          environment, bridge, true) getOrElse { fail("Unable to createTargetPlatformAndRepositories") }
      targetPlatform should not be (null)
      repositories should not be (null)
      info("Repositories: " + repositories.mkString(","))
      val resolver = bridge.p2ResolverFactory.createResolver(new MavenLoggerAdapter(bridge.plexus.getLogger, true))
      resolver should not be (null)
      resolver.addDependency(dependencies.head.getType(), dependencies.head.getArtifactId(), dependencies.head.getVersion())
      val resolutionResult = resolver.resolveDependencies(targetPlatform, null)
      resolutionResult should not be (null)
      val artifacts = (for (r ← resolutionResult) yield r.getArtifacts()).flatten

      // Process results
      val rePerDependencyMap = ResolveP2.inner.asInstanceOf[TestResolveP2].collectArtifactsPerDependency(dependencies, artifacts)
      artifacts.foreach { entry ⇒
        val originModuleIds = rePerDependencyMap.get(entry).map(dependencies ⇒ dependencies.flatMap(Dependency.getOrigin)) getOrElse Seq()
        entry.getInstallableUnits().map(_ match {
          case riu: IInstallableUnit if originModuleIds.nonEmpty && originModuleIds.exists(_.withSources) ⇒
            info("Collect P2 IU %s with source code".format(riu))
          case riu: IInstallableUnit if originModuleIds.nonEmpty ⇒
            info("Collect P2 IU %s".format(riu))
          case riu: IInstallableUnit ⇒
            info("Collect an unbound installable unit: " + riu)
          case ru ⇒
            info("Skip an unknown reactor unit: " + ru)
        })
      }

      artifacts.size should be(57)
    }
  }

  object FakeState {
    lazy val settings: Seq[Def.Setting[_]] = MavenTest.this.settings

    val base = new File("").getAbsoluteFile
    val testProject = Project("test-project", base)

    val currentProject = Map(testProject.base.toURI -> testProject.id)
    val currentEval: () ⇒ sbt.compiler.Eval = () ⇒ Load.mkEval(Nil, base, Nil)
    val sessionSettings = SessionSettings(base.toURI, currentProject, Nil, Map.empty, Nil, currentEval)

    val delegates: (Scope) ⇒ Seq[Scope] = scope ⇒ Seq(scope, Scope(This, scope.config, This, This))
    val scopeLocal: Def.ScopeLocal = _ ⇒ Nil

    val data: Settings[Scope] = Def.make(settings)(delegates, scopeLocal, Def.showFullKey)
    val extra: KeyIndex ⇒ BuildUtil[_] = (keyIndex) ⇒ BuildUtil(base.toURI, Map.empty, keyIndex, data)
    val structureIndex: StructureIndex = Load.structureIndex(data, settings, extra, Map.empty)
    val streams: (State) ⇒ BuildStreams.Streams = null

    val loadedDefinitions: LoadedDefinitions = new LoadedDefinitions(
      base, Nil, ClassLoader.getSystemClassLoader, Nil, Seq(testProject), Nil)

    val pluginData = PluginData(Nil, Nil, None, None, Nil)
    val detectedModules: DetectedModules[Plugin] = new DetectedModules(Nil)
    val builds: DetectedModules[Build] = new DetectedModules[Build](Nil)

    val detectedAutoPlugins: Seq[DetectedAutoPlugin] = Seq.empty
    val detectedPlugins = new DetectedPlugins(detectedModules, detectedAutoPlugins, builds)
    val loadedPlugins = new LoadedPlugins(base, pluginData, ClassLoader.getSystemClassLoader, detectedPlugins)
    val buildUnit = new BuildUnit(base.toURI, base, loadedDefinitions, loadedPlugins)

    val (partBuildUnit: PartBuildUnit, _) = Load.loaded(buildUnit)
    val loadedBuildUnit = Load.resolveProjects(base.toURI, partBuildUnit, _ ⇒ testProject.id)

    val units = Map(base.toURI -> loadedBuildUnit)
    val buildStructure = new BuildStructure(units, base.toURI, settings, data, structureIndex, streams, delegates, scopeLocal)

    val attributes = AttributeMap.empty ++ AttributeMap(
      AttributeEntry(Keys.sessionSettings, sessionSettings),
      AttributeEntry(Keys.stateBuildStructure, buildStructure))

    val initialGlobalLogging = GlobalLogging.initial(MainLogging.globalDefault(ConsoleOut.systemOut), File.createTempFile("sbt", ".log"), ConsoleOut.systemOut)
    val commandDefinitions = BasicCommands.allBasicCommands
    val state = State(null, commandDefinitions, Set.empty, None, Seq.empty, State.newHistory,
      attributes, initialGlobalLogging, State.Continue)
  }
  class TestResolveP2 extends ResolveP2 {
    override def createTargetPlatformAndRepositories(p2Pepositories: Seq[(String, URI)],
      environment: ExecutionEnvironmentConfigurationStub, maven: Maven,
      includeLocalMavenRepo: Boolean)(implicit arg: Plugin.TaskArgument): Option[(TargetPlatform, Seq[IArtifactRepository])] =
      super.createTargetPlatformAndRepositories(p2Pepositories, environment, maven, includeLocalMavenRepo)

    override def collectArtifactsPerDependency(dependencies: Seq[MavenDependency],
      artifacts: Seq[P2ResolutionResult.Entry])(implicit arg: Plugin.TaskArgument): immutable.HashMap[P2ResolutionResult.Entry, Seq[MavenDependency]] =
      super.collectArtifactsPerDependency(dependencies, artifacts)
  }
}
