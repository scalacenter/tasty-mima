package tastymima.intf;

import java.net.URL;
import java.net.URLClassLoader;

final class ReflectionLoaderImpl {
  private ReflectionLoaderImpl() {
  }

  static TastyMiMa newInstance(URL[] tastyMiMaClasspath, ClassLoader parent) {
    try {
      ClassLoader filteredParent = new FilteringClassLoader(parent);
      ClassLoader loader = new URLClassLoader(tastyMiMaClasspath, filteredParent);
      Class<?> clazz = Class.forName("tastymima.TastyMiMa", true, loader);
      return (TastyMiMa) clazz.newInstance();
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Cannot load the TastyMiMa interface", e);
    } catch (InstantiationException e) {
      throw new RuntimeException("Cannot load the TastyMiMa interface", e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Cannot load the TastyMiMa interface", e);
    }
  }

  private static final class FilteringClassLoader extends ClassLoader {
    FilteringClassLoader(ClassLoader parent) {
      super(parent);
    }

    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
      if (name.startsWith("tastymima.intf.")
          || name.startsWith("java.")
          || name.startsWith("sun.misc.")
          || name.startsWith("sun.reflect.")
          || name.startsWith("jdk.internal.reflect.")) {
        return super.loadClass(name, resolve);
      } else {
        return null;
      }
    }
  }
}
