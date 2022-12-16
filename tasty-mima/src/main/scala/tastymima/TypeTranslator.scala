package tastymima

import tastyquery.Contexts.*
import tastyquery.Flags.*
import tastyquery.Names.*
import tastyquery.Symbols.*
import tastyquery.Types.*

private[tastymima] final class TypeTranslator(oldCtx: Context, newCtx: Context):
  private val translatedRecTypes = new java.util.IdentityHashMap[Type, Type]()

  def translateType(oldType: Type): Type =
    val alreadyTranslated = translatedRecTypes.get(oldType)
    if alreadyTranslated != null then alreadyTranslated
    else
      oldType match
        case oldType: NamedType =>
          oldType.prefix match
            case oldPrefix: Type =>
              val translatedPrefix = translateType(oldPrefix)
              NamedType(translatedPrefix, oldType.name)(using newCtx)
            case NoPrefix =>
              throw NotImplementedError(s"cannot translate local ref $oldType")

        case oldType: PackageRef =>
          PackageRef(oldType.fullyQualifiedName)

        case oldType: ThisType =>
          ThisType(translateType(oldType.tref).asInstanceOf[TypeRef])

        case oldType: AppliedType =>
          AppliedType(translateType(oldType.tycon), oldType.args.map(translateType(_)))

        case oldType: MethodType =>
          MethodType(oldType.paramNames)(
            mt => {
              translatedRecTypes.put(oldType, mt)
              oldType.paramTypes.map(translateType(_))
            },
            mt => translateType(oldType.resultType)
          )

        case _ =>
          throw NotImplementedError(oldType.toString())
  end translateType
end TypeTranslator
