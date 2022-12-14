package tastymima

import tastyquery.Symbols.PackageSymbol

import tastyquery.Contexts.*
import tastyquery.Symbols.*

private[tastymima] final class Translator(oldCtx: Context, newCtx: Context):
  def translatePackage(packageSym: PackageSymbol): PackageSymbol =
    newCtx.findPackageFromRoot(packageSym.fullName)
end Translator
