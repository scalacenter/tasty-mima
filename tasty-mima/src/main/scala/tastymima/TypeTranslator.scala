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
              oldType.symbol(using oldCtx) match
                case oldSym: ClassTypeParamSymbol =>
                  translateClassTypeParamRef(translatedPrefix, oldSym)
                case _ =>
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

        case oldType: PolyType =>
          PolyType(oldType.paramNames)(
            pt => {
              translatedRecTypes.put(oldType, pt)
              oldType.paramTypeBounds.map(translateTypeBounds(_))
            },
            pt => translateType(oldType.resultType)
          )

        case oldType: TypeParamRef =>
          val translatedBinders = translatedRecTypes.get(oldType.binders).nn.asInstanceOf[TypeLambdaType]
          TypeParamRef(translatedBinders, oldType.paramNum)

        case _ =>
          throw NotImplementedError(s"$oldType of ${oldType.getClass()}")
  end translateType

  private def translateTypeBounds(oldBounds: TypeBounds): TypeBounds = oldBounds match
    case RealTypeBounds(low, high) => RealTypeBounds(translateType(low), translateType(high))
    case TypeAlias(alias)          => TypeAlias(translateType(alias))
  end translateTypeBounds

  private def translateClassTypeParamRef(translatedPrefix: Type, oldSym: ClassTypeParamSymbol): TypeRef =
    val typeParamIndex = withOldCtx {
      oldSym.owner.typeParams.indexOf(oldSym)
    }
    val translatedSym = withNewCtx {
      translatedPrefix match
        case translatedPrefix: ThisType =>
          translatedPrefix.cls.typeParams(typeParamIndex)
        case _ =>
          throw NotImplementedError(s"cannot translate class type param ref with non-this prefix $translatedPrefix")
    }
    TypeRef(translatedPrefix, translatedSym)
  end translateClassTypeParamRef

  private def withOldCtx[A](f: Context ?=> A): A = f(using oldCtx)

  private def withNewCtx[A](f: Context ?=> A): A = f(using newCtx)
end TypeTranslator
